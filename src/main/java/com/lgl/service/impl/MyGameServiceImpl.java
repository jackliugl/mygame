package com.lgl.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.lgl.model.CanvasMessage;
import com.lgl.model.ChatMessage;
import com.lgl.model.GameMessage;
import com.lgl.model.GameMessageType;
import com.lgl.model.GameStatus.ActiveUser;
import com.lgl.service.MyCanvasService;
import com.lgl.service.MyChatService;
import com.lgl.service.MyGameService;

@Component
public class MyGameServiceImpl implements MyGameService {

	SimpMessagingTemplate simpMessagingTemplate = null;

	Timer timer = null;

	List<String> allWordList = new ArrayList<String>();
	
	boolean ghostStatusChanged = false;

	public static Map<String, String> userIconMap;
	static {
		userIconMap = new HashMap<String, String>();
		userIconMap.put("erica", 	"erica.png");
		userIconMap.put("elaine", 	"elaine.png");
		userIconMap.put("jack", 	"jack.png");
		userIconMap.put("peter", 	"boy.png");
		userIconMap.put("tom", 		"boy.png");
		userIconMap.put("claire", 	"claire.png");
		userIconMap.put("hellen", 	"girl.png");
	};
	
	public MyGameServiceImpl() {
		super();
		
		new Timer().scheduleAtFixedRate(new GhostStatusCheckTimerTask(this), 10000, 10000);
	}

	public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate) {
		this.simpMessagingTemplate = simpMessagingTemplate;
	}

	public SimpMessagingTemplate getSimpMessagingTemplate() {
		return simpMessagingTemplate;
	}

	@Override
	public String getUserIcon(String name) {
		return userIconMap.containsKey(name) ? userIconMap.get(name) : "";
	}

	@Override
	public CanvasMessage processCanvasMessage(String sessionID, CanvasMessage message) {

		// clear request
		if (message.isClear()) {
			synchronized (syncList) {
				// clear history
				syncList.clear();
			}
		}

		// set session id
		message.setSessionID(sessionID);

		synchronized (syncList) {
			// add to list to keep history
			syncList.add(message);
		}

		return message;
	}

	@Override
	public void sendMessage(String sessionID, CanvasMessage message) {
		simpMessagingTemplate.convertAndSend("/topic/gameCanvas", message);
	}

	@Override
	public void sendMessage(String sessionID, GameMessage message) {
		message.setSessionID(sessionID);
		simpMessagingTemplate.convertAndSend("/topic/gameStatus", message);
	}

	@Override
	public void sendMessageToAllOthers(String sessionID, GameMessage message) {
		message.setSessionID(sessionID);

		activeUserMap.keySet().stream().filter(x -> !x.equals(sessionID)).forEach(x -> {
			System.out.println("sending to session: " + x);
			simpMessagingTemplate.convertAndSend("/topic/gameStatus/" + x, message);
		});
	}

	@Override
	public void sendMessageToUser(String sessionID, GameMessage message) {
		message.setSessionID(sessionID);

		simpMessagingTemplate.convertAndSend("/topic/gameStatus/" + sessionID, message);
	}

	@Override
	public void leave(String sessionID) {

		if (Optional.ofNullable(activeUserMap.get(sessionID)).isPresent()) {

			String name = MyGameService.activeUserMap.get(sessionID).getName();

			MyGameService.super.leave(sessionID);

			GameMessage msg = new GameMessage(GameMessageType.SERVER_USER_DISCONNECT);
			msg.setMessage(String.format("%s left the game", name));

			sendMessageToAllOthers(sessionID, msg);

			// if startedUser left the game, then game over
			// if less than 2 people, game over
			if (gameStatus.getStartedUser().equals(name) && gameStatus.isStarted()) {

				// game over
				sendGameOverMessage(String.format("%s left, <span style='color:red;font-size:24px'>GAME OVER!</span>", name));

			} else if (gameStatus.isStarted() && activeUserMap.values().stream().filter(x -> !x.isGhost()).count() < 2) {
				
				// game over
				sendGameOverMessage(String.format("only 1 active player left, <span style='color:red;font-size:24px'>GAME OVER!</span>"));
			}

			System.out.println(String.format("%s left the game", name));
		}
	}

	public void clearCanvas() {

		synchronized (syncList) {
			// clear history
			syncList.clear();
		}

		// send clear request
		CanvasMessage msg = new CanvasMessage();
		msg.setClear(true);
		this.sendMessage("", msg);

	}

	public GameMessage processLoginReq(GameMessage req) {

		String sessionID = req.getSessionID();

		// send USER_LOGIN_RESP
		if (activeUserMap.values().stream().filter(x -> req.getName().equals(x.getName())).findFirst().isPresent()) {
			GameMessage msg = new GameMessage(GameMessageType.USER_LOGIN_RESP);
			msg.setMessage(String.format("%s is already taken", req.getName()));
			this.sendMessage(sessionID, msg);
			return null;
		}

		this.join(sessionID, req.getName());

		GameMessage msg = new GameMessage(GameMessageType.USER_LOGIN_RESP);
		msg.setMessage(String.format("%s joined the game", req.getName()));
		msg.setName(req.getName());

		this.sendMessage(sessionID, msg);

		// send all historical CanvasMessages
		synchronized (syncList) {
			syncList.forEach(x -> sendMessage(sessionID, x));
		}

		return null;
	}

	private void loadAllWordList() {
		try {
			// Note: below won't work if easy.txt is inside the jar
			/*
			 * Resource resource = new ClassPathResource("easy.txt"); List<String> list =
			 * Files.readAllLines(resource.getFile().toPath(), Charset.defaultCharset() );
			 */

			List<String> list = null;
			InputStream resource;
			resource = new ClassPathResource("wordList.txt").getInputStream();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
				list = reader.lines().collect(Collectors.toList());
			}

			this.allWordList.clear();
			this.allWordList.addAll(list);
			System.out.println(String.format("loadAllWordList: %d words loaded from resource/wordList.txt", list.size()));
			
			// try load other files
			String file = "/home/pi/mygame/allWordList.txt";
			try {
		        Path path = Paths.get(file);
		        List<String> lines = Files.readAllLines(path);
		        this.allWordList.addAll(lines);
		        System.out.println(String.format("loadAllWordList: %d words loaded from %s", lines.size(), file));
			} catch (IOException ex) {
				System.out.println(String.format("loadAllWordList: %s not found", file));
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public GameMessage processStartGameReq(GameMessage req) {

		// load all word list
		loadAllWordList();

		synchronized (gameStatus) {
			if (gameStatus.isStarted()) {
				System.out.println(
						String.format("*** The game has already been started by %s", gameStatus.getDrawingUser()));
				return null;
			}

			// update game status
			gameStatus.setStartedUser(req.getName());
			gameStatus.setCurrentRound(1);
			gameStatus.setDrawingUser(req.getName());
			gameStatus.setDrawingWord("");
			gameStatus.setStarted(true);
			gameStatus.setTimeRemaining(60);

			// reset user status
			MyGameServiceImpl.activeUserMap.values().forEach(x -> {
				x.setScore(0);
				x.setDone(false);
			});

			clearCanvas();
		}

		GameMessage msg = new GameMessage(GameMessageType.USER_START_GAME_RESP);
		msg.setMessage(String.format("%s started the game", req.getName()));
		this.sendMessage(req.getSessionID(), msg);

		return null;
	}

	@Override
	public GameMessage processGameMessage(String sessionID, GameMessage req) {

		GameMessage respMsg = null;

		if (req.getSessionID() == null || req.getSessionID() == "") {
			req.setSessionID(sessionID);
		}

		switch (req.getType()) {
		case USER_LOGIN_REQ:
			respMsg = processLoginReq(req);
			break;
		case USER_START_GAME_REQ:
			respMsg = processStartGameReq(req);
			break;
		case USER_WORD_LIST_REQ:
			respMsg = processWordListReq(req);
			break;
		case USER_CHOOSE_WORD_REQ:
			respMsg = processChooseWordReq(req);
			break;
		case USER_GUESS_WORD_REQ:
			respMsg = processGuessWordReq(req);
			break;
		case USER_SERVER_HEARTBEAT:
			respMsg = processHeartbeatReq(req);
			break;
		default:
			System.out.println("**** UNKNOWN Game Message type: " + req.getType());
		}

		return respMsg;
	}

	private GameMessage processHeartbeatReq(GameMessage req) {
		
		synchronized(activeUserMap) {
			this.ghostStatusChanged = false;
	
			// update user lastTS
			ActiveUser user = activeUserMap.get(req.getSessionID());
			if (user != null) {
				user.setLastTS(System.currentTimeMillis());
				
				if (user.isGhost()) {
					user.setGhost(false);
					System.out.println(String.format("ghost status for %s -> false", user.getName()));
					ghostStatusChanged = true;
				}
			}
		
			// send server status message if there is any status change
			if (ghostStatusChanged) {
				this.sendMessage(req.getSessionID(), new GameMessage(GameMessageType.SERVER_GAME_STATUS));
			}
		}
		
		return null;
	}

	private GameMessage processGuessWordReq(GameMessage req) {

		req.setWordGuessed(req.getMessage().trim());
		
		System.out.println(String.format("From: [%s] -> drawingUser: [%s], drawingWord: [%s], guessedWord: [%s]", 
				req.getName(), gameStatus.getDrawingUser(), gameStatus.getDrawingWord(), req.getWordGuessed()));
		
		// update score if non-drawing user guessed correct (case-insensitive)
		if (gameStatus.isStarted() && req.getWordGuessed().equalsIgnoreCase(gameStatus.getDrawingWord())
				&& !req.getName().equals(gameStatus.getDrawingUser())) {

			if (gameStatus.getGuessedUsers().contains(req.getName())) {
				// already guessed correct
				GameMessage msg = new GameMessage(GameMessageType.USER_GUESS_WORD_RESP);
				msg.setMessage(String.format("<span style='color:red' class='blinkingOnce'><b>%s</b> already guessed correct!</span>", req.getName()));
				this.sendMessage(req.getSessionID(), msg);
				return null;
			}
			
			updateUserScore(req);

			GameMessage msg = new GameMessage(GameMessageType.USER_GUESS_WORD_RESP);
			msg.setMessage(String.format("<span style='color:green' class='blinkingOnce'><b>%s</b> guessed correct!</span>", req.getName()));
			this.sendMessage(req.getSessionID(), msg);

			// check if all non-ghost users have guessed (except the drawing user)
			List<String> realActiveUsers = activeUserMap.values().stream().
				filter(x -> !x.isGhost() && !x.getName().equals(gameStatus.getDrawingUser()))
				.map(ActiveUser::getName)
				.collect(Collectors.toList());
			
			
			gameStatus.getGuessedUsers().add(req.getName());
			
			System.out.println("realActiveUsers: " + realActiveUsers.stream().collect(Collectors.joining(",")));
			System.out.println("   GuessedUsers: " + gameStatus.getGuessedUsers().stream().collect(Collectors.joining(",")) );
					
			//if (gameStatus.getGuessedUsers().size() == (activeUserMap.size() - 1)) {
			if (gameStatus.getGuessedUsers().containsAll(realActiveUsers)) {
				timer.cancel();

				// skip to next user
				skipToNextUser();
			}

		} else {
			GameMessage msg = new GameMessage(GameMessageType.SERVER_GAME_STATUS);
			msg.setMessage(String.format("<b>%s</b>: %s", req.getName(), req.getMessage()));
			msg.setName(req.getName());
			this.sendMessage(req.getSessionID(), msg);
		}

		return null;
	}

	private void updateUserScore(GameMessage req) {

		int timeLeft = gameStatus.getTimeRemaining();
		int guessUserScore = 0;
		int drawingUserScore = 0;

		if (timeLeft >= 45) {
			
			guessUserScore = 400;
			drawingUserScore = 300;
			
		} else if (timeLeft >= 30) {
			
			guessUserScore = 300;
			drawingUserScore = 200;
			
		} else if (timeLeft >= 15) {
			
			guessUserScore = 200;
			drawingUserScore = 100;
			
		} else {
			
			guessUserScore = 100;
			drawingUserScore = 50;
		}

		// add score to user
		ActiveUser user = activeUserMap.get(req.getSessionID());
		user.setScore(user.getScore() + guessUserScore);

		// add score to drawing user
		final int score = drawingUserScore;
		activeUserMap.values().stream().filter(x -> x.getName().equals(gameStatus.getDrawingUser())).forEach(x -> {
			x.setScore(x.getScore() + score);
		});

	}

	private GameMessage processWordListReq(GameMessage req) {
		if (!gameStatus.isStarted()) {
			System.out.println(String.format("*** The game has not started yet"));
			return null;
		}

		GameMessage msg = new GameMessage(GameMessageType.USER_WORD_LIST_RESP);
		// msg.setWordList(Arrays.asList("dog", "cat", "snake", "rabbit"));
		msg.setWordList(getRandomWordList());

		this.sendMessageToUser(req.getSessionID(), msg);
		return null;
	}

	List<String> getRandomWordList() {

		List<String> retList = new ArrayList<>();

		while (true) {
			Random random = new Random();
			int index = random.nextInt(this.allWordList.size());
			String word = this.allWordList.get(index);

			if (retList.contains(word)) {
				continue;
			}

			retList.add(word);
			if (retList.size() == 4) {
				break;
			}
		}

		return retList;
	}

	private GameMessage processChooseWordReq(GameMessage req) {
		synchronized (gameStatus) {
			if (!gameStatus.isStarted()) {
				System.out.println(String.format("*** The game has not started yet"));
				return null;
			}

			if (!gameStatus.getDrawingUser().equals(req.getName())) {
				System.out.println(String.format("drawing user is %s, ignore ChooseWordReq from %s", gameStatus.getDrawingUser(), req.getName()));
				return null;
			}
			
			gameStatus.getGuessedUsers().clear();

			String word = req.getWordChosen();
			if (!StringUtils.isEmpty(word)) {
				gameStatus.setDrawingWord(req.getWordChosen());
				gameStatus.setDrawingWordMask(word.replaceAll("[^ -]", "_"));
			} else {
				// skip to next user
				skipToNextUser();
				return null;
			}

			clearCanvas();
		}

		GameMessage msg = new GameMessage(GameMessageType.USER_CHOOSE_WORD_RESP);
		this.sendMessageToUser(req.getSessionID(), msg);

		// start timer
		timer = new Timer();
		timer.scheduleAtFixedRate(new MyTimerTask(this), 1000, 1000);

		return null;
	}

	private void skipToNextUser() {

		// mark current drawing user as done
		MyGameService.activeUserMap.values().stream().filter(x -> x.getName().equals(gameStatus.getDrawingUser()))
				.forEach(x -> {
					x.setDone(true);
				});

		if (gameStatus.getDrawingWord() != "") {
			// send the correct answer of previous round
			GameMessage msg = new GameMessage(GameMessageType.SERVER_GAME_STATUS);
			msg.setMessage(String.format("The answer is: <span style='color:blue'>%s</span>", gameStatus.getDrawingWord()));
			this.sendMessage("", msg);
		}
		
		// find next user that is not ghost
		Optional<ActiveUser> nextUser = MyGameService.activeUserMap.values().stream().filter(x -> !x.isDone() && !x.isGhost())
				.findFirst();
		if (nextUser.isPresent()) {

			gameStatus.setDrawingUser(nextUser.get().getName());
			gameStatus.setDrawingWord("");
			gameStatus.setDrawingWordMask("");
			gameStatus.setTimeRemaining(60);

			GameMessage msg = new GameMessage(GameMessageType.SERVER_NEXT_USER_REQ);
			this.sendMessage("", msg);

		} else if (gameStatus.getCurrentRound() < gameStatus.getTotalRounds()) { // next round

			gameStatus.setCurrentRound(gameStatus.getCurrentRound() + 1);
			gameStatus.setDrawingUser("");
			gameStatus.setDrawingWord("");
			gameStatus.setDrawingWordMask("");
			gameStatus.setTimeRemaining(60);

			// reset user status for next round
			MyGameServiceImpl.activeUserMap.values().forEach(x -> {
				x.setDone(false);
			});

			// new round always starts from the one who started the game
			// if startedUser is not available or ghost, then find next user
			String firstDrawingUser = gameStatus.getStartedUser();
			if (activeUserMap.values().stream()
					.filter(x -> x.getName().equals(firstDrawingUser) && !x.isGhost()).count() > 0) {
				
				gameStatus.setDrawingUser(firstDrawingUser);
				
			} else {
				
				// find next non-ghost user
				activeUserMap.values().stream().filter(x -> !x.isGhost()).findFirst().ifPresent(x -> {
					gameStatus.setDrawingUser(x.getName());
				});
			}
			
			GameMessage msg = new GameMessage(GameMessageType.SERVER_NEXT_USER_REQ);
			this.sendMessage("", msg);

		} else {

			sendGameOverMessage("<span style='color:red;font-size:24px'>GAME OVER!</span>");
		}
	}

	void sendGameOverMessage(String message) {
		gameStatus.setStarted(false);

		if (timer != null) timer.cancel();
		
		// game over
		GameMessage msg = new GameMessage(GameMessageType.SERVER_GAME_OVER);

		// find the winner
		int maxScore = activeUserMap.values().stream().mapToInt(x -> x.getScore()).max().orElse(0);
		if (maxScore > 0) {
			List<String> winners = activeUserMap.values().stream().filter(x -> x.getScore() == maxScore)
					.map(x -> x.getName()).collect(Collectors.toList());

			msg.setName(StringUtils.join(winners, ','));
		}

		msg.setMessage(message);
		this.sendMessage("", msg);		
	}
	
	class MyTimerTask extends TimerTask {

		MyGameServiceImpl gameService = null;

		MyTimerTask(MyGameServiceImpl gameService) {
			this.gameService = gameService;
		}

		@Override
		public void run() {

			// time out
			if (gameStatus.getTimeRemaining() == 0) {
				timer.cancel();
				gameService.skipToNextUser();
				return;
			}

			int timeLeft = gameStatus.getTimeRemaining();
			if (timeLeft == 15 || timeLeft == 30 || timeLeft == 45) {

				String dw = gameStatus.getDrawingWord();
				String dm = gameStatus.getDrawingWordMask();

				// find the first masked index
				int index = -1;
				if (dm.contains("_")) {
					while (true) {
						Random random = new Random();
						index = random.nextInt(dw.length());
						if (dm.charAt(index) == '_') {
							break;
						}
					}

					if (index >= 0) {
						String mask = dm.substring(0, index) + dw.charAt(index) + dm.substring(index + 1);

						// make sure at least one letter is masked
						if (mask.contains("_")) {
							gameStatus.setDrawingWordMask(mask);
						}
					}
				}
			}

			gameStatus.setTimeRemaining(gameStatus.getTimeRemaining() - 1);

			GameMessage msg = new GameMessage(GameMessageType.SERVER_GAME_STATUS);
			gameService.sendMessage("", msg);
		}

	}
	
	class GhostStatusCheckTimerTask extends TimerTask {

		MyGameServiceImpl gameService = null;

		GhostStatusCheckTimerTask(MyGameServiceImpl gameService) {
			this.gameService = gameService;
		}

		@Override
		public void run() {

			synchronized(activeUserMap) {
				
				ghostStatusChanged = false;
				
				// set user ghost status if idle time > 45
				activeUserMap.values().forEach(x -> {
					if ( !x.isGhost() && (System.currentTimeMillis() - x.getLastTS()) > 45000) {
						x.setGhost(true);
						ghostStatusChanged = true;
						System.out.println(String.format("GhostStatusCheckTimerTask: ghost status for %s -> true", x.getName()));
					}
				});
				
				// send server status message if there is any status change
				if (ghostStatusChanged) {
					gameService.sendMessage("", new GameMessage(GameMessageType.SERVER_GAME_STATUS));
				}
				
				if (gameStatus.isStarted()) {
					
					// send game over if less than 2 real active users
					if (gameStatus.isStarted() && activeUserMap.values().stream().filter(x -> !x.isGhost()).count() < 2) {
						
						gameService.sendGameOverMessage("only 1 active player left, <span style='color:red;font-size:24px'>GAME OVER!</span>");
						
					} else if (!gameStatus.getDrawingUser().equals("") && !isUserActive(gameStatus.getDrawingUser()) && gameStatus.getDrawingWord().equals("") ) {
					
						// skip to next user if drawingUser is ghost and is pending choose word
						gameService.skipToNextUser();
					}
				}
			}
		}

	}
	
	// return true if ghost is false
	boolean isUserActive(String name) {
		if (activeUserMap.values().stream().filter(x -> x.getName().equals(name) && !x.isGhost()).count() > 0) {
			return true;
		}
		
		return false;
	}
}
