// Fig. 24.15: TicTacToeClient.java
// Client that let a user play Tic-Tac-Toe with another across a network.
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TicTacToeClient extends JFrame implements Runnable 
{
   private JTextField idField; // textfield to display player's mark
   private JTextArea displayArea; // JTextArea to display output
   private JPanel boardPanel; // panel for tic-tac-toe board
   private JPanel panel2; // panel to hold board
   private Square board[][]; // tic-tac-toe board
   private Square currentSquare; // current square
   private String myMark; // this client's mark
   private boolean myTurn; // determines which client's turn it is
   private final String X_MARK = "X"; // mark for first client
   private final String O_MARK = "O"; // mark for second client
   private final int bsize = 16;
   private String baseID = "id";
   private String messageDeduplicationId = "id";
   private static String QUEUE_RECEIVER;
   private static String QUEUE_SENDER;
   private int randID;
   public boolean gameOver = false;
   private static final String FIFO1 = "https://sqs.us-west-2.amazonaws.com/296938127935/fifo1.fifo";
   private static final String FIFO2 = "https://sqs.us-west-2.amazonaws.com/296938127935/fifo2.fifo";
  private Random rand;
  private AmazonSQS sqs;

   // set up user-interface and board
   public TicTacToeClient( String host )
   { 
	   
	   rand = new Random();
	   randID = rand.nextInt(2000000000);
	   messageDeduplicationId = baseID + randID;
	   AWSCredentials credentials = null;
       try {
           credentials = new ProfileCredentialsProvider("default").getCredentials();
       } catch (Exception e) {
           throw new AmazonClientException(
                   "Cannot load the credentials from the credential profiles file. " +
                   "Please make sure that your credentials file is at the correct " +
                   "location (/Users/ytian/.aws/credentials), and is in valid format.",
                   e);
       }
       
       sqs = AmazonSQSClientBuilder.standard()
    		   .withCredentials(new AWSStaticCredentialsProvider(credentials))
    		   .withRegion(Regions.US_WEST_2)
    		   .build();
       
       
	   
       /*ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL)
    		   .withWaitTimeSeconds(10)
    		   .withMaxNumberOfMessages(10);*/
    		  
       QUEUE_RECEIVER = FIFO2;
       QUEUE_SENDER = FIFO1;
      displayArea = new JTextArea( 4, 30 ); // set up JTextArea
      displayArea.setEditable( false );
      add( new JScrollPane( displayArea ), BorderLayout.SOUTH );

      boardPanel = new JPanel(); // set up panel for squares in board
      boardPanel.setLayout( new GridLayout( bsize, bsize, 0, 0 ) ); //was 3

      board = new Square[ bsize ][ bsize ]; // create board
      
      // loop over the rows in the board
      for ( int row = 0; row < board.length; row++ ) 
      {
         // loop over the columns in the board
         for ( int column = 0; column < board[ row ].length; column++ ) 
         {
            // create square. initially the symbol on each square is a white space.
            board[ row ][ column ] = new Square( " ", row * bsize + column );
            boardPanel.add( board[ row ][ column ] ); // add square       
         } // end inner for
      } // end outer for

      idField = new JTextField(); // set up textfield
      idField.setEditable( false );
      add( idField, BorderLayout.NORTH );
      
      panel2 = new JPanel(); // set up panel to contain boardPanel
      panel2.add( boardPanel, BorderLayout.CENTER ); // add board panel
      add( panel2, BorderLayout.CENTER ); // add container panel

      setSize( 600, 600 ); // set size of window
      setVisible( true ); // show window
      
      System.out.println("Receiving messages from MyFifoQueue.fifo.\n");
      //emptyQueue();
      final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(FIFO2).withWaitTimeSeconds(5);

      
      List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
      for (final Message message : messages) {
          System.out.println("Message from constructor");
          System.out.println("  MessageId:     " + message.getMessageId());
          System.out.println("  ReceiptHandle: " + message.getReceiptHandle());
          System.out.println("  MD5OfBody:     " + message.getMD5OfBody());
          System.out.println("  Body:          " + message.getBody());
          for (final Entry<String, String> entry : message.getAttributes().entrySet()) {
              System.out.println("Attribute");
              System.out.println("  Name:  " + entry.getKey());
              System.out.println("  Value: " + entry.getValue());
          }
          //processMessage(message.getBody());
      }
      if(!messages.isEmpty()) {
    	  System.out.println("messages list not empty");
    	  deleteMessage(messages);
    	  QUEUE_RECEIVER = FIFO2;
    	  QUEUE_SENDER = FIFO1;
    	  myMark = O_MARK;
    	  displayMessage("Player O connected, please wait\n");
    	  sendMessage("connected");
      }
      else {
    	  myMark = X_MARK;
    	  System.out.println("Setting mark to "+myMark);
    	  QUEUE_RECEIVER = FIFO1;
    	  QUEUE_SENDER = FIFO2;
    	  displayMessage("Player X connected\n");
    	  sendMessage("connected");
    	  displayMessage("Waiting for another player\n");
    	  
      }
      startClient();
   } // end TicTacToeClient constructor
   
   public void deleteMessage(List<Message> messages) {
	      System.out.println("Deleting the message: "+messages.get(0).getBody());
	      String messageReceiptHandle = messages.get(0).getReceiptHandle();
	      sqs.deleteMessage(new DeleteMessageRequest(QUEUE_RECEIVER, messageReceiptHandle));
   }
   public void emptyQueue() {
	   while(true) {
		   boolean t = true;
		   ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_RECEIVER).withWaitTimeSeconds(5);

		   List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
		      for (Message message : messages) {
		          System.out.println("Message");
		          System.out.println("  MessageId:     " + message.getMessageId());
		          System.out.println("  ReceiptHandle: " + message.getReceiptHandle());
		          System.out.println("  MD5OfBody:     " + message.getMD5OfBody());
		          System.out.println("  Body:          " + message.getBody());
		          for (final Entry<String, String> entry : message.getAttributes().entrySet()) {
		              System.out.println("Attribute");
		              System.out.println("  Name:  " + entry.getKey());
		              System.out.println("  Value: " + entry.getValue());
		          }
		      }
		      if(!messages.isEmpty()) {
		    	  final String messageReceiptHandle = messages.get(0).getReceiptHandle();
		    	  sqs.deleteMessage(new DeleteMessageRequest(QUEUE_RECEIVER, messageReceiptHandle));
		      }
		      else 
		    	  t = false;
		      
		      receiveMessageRequest = new ReceiveMessageRequest(QUEUE_SENDER).withWaitTimeSeconds(5);

			   messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			      for (Message message : messages) {
			          System.out.println("Message");
			          System.out.println("  MessageId:     " + message.getMessageId());
			          System.out.println("  ReceiptHandle: " + message.getReceiptHandle());
			          System.out.println("  MD5OfBody:     " + message.getMD5OfBody());
			          System.out.println("  Body:          " + message.getBody());
			          for (final Entry<String, String> entry : message.getAttributes().entrySet()) {
			              System.out.println("Attribute");
			              System.out.println("  Name:  " + entry.getKey());
			              System.out.println("  Value: " + entry.getValue());
			          }
			      }
			      if(!messages.isEmpty()) {
			    	  String messageReceiptHandle = messages.get(0).getReceiptHandle();
			    	  sqs.deleteMessage(new DeleteMessageRequest(QUEUE_SENDER, messageReceiptHandle));
			      }
			      else
			    	  t = false;
			      if(t)
			    	  break;
		      
	   }
   }
   // start the client thread
   public void startClient()
   {  
	   System.out.println("Starting client");
      // create and start worker thread for this client
      ExecutorService worker = Executors.newFixedThreadPool( 1 );
      worker.execute( this ); // execute client
   } // end method startClient

   public void sendMessage(String msg) {
	   randID ++;
	   messageDeduplicationId = baseID + randID;
	   SendMessageRequest sendMessageFifoQueue = new SendMessageRequest()
    		   .withQueueUrl(QUEUE_SENDER)
    		   .withMessageBody(msg)
    		   .withMessageGroupId("baeldung-group-1")
    		   .withMessageDeduplicationId(messageDeduplicationId);
       System.out.println("sending: "+msg+" to: "+QUEUE_SENDER);
	   sqs.sendMessage(sendMessageFifoQueue);
   }
   // control thread that allows continuous update of displayArea
   public void run()
   {
      //myMark =  "X"; //Get player's mark (X or O). We hard coded here in demo. In your implementation, you may get this mark dynamically 
                     //from the cloud service. This is the initial state of the game.
	   if(myMark.equals(X_MARK)) {
		   while(true) {
			  ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_RECEIVER);
	 		  List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
	 		  if(!messages.isEmpty()) {
	 			 System.out.println("wait loop: "+messages.get(0).getBody());
	 		  }
	 	      if(!messages.isEmpty() ) {
	 	    	  System.out.println("RESPONSE PLAYER 2 CONNECTED:"+messages.get(0).getBody());
	 	    	  displayMessage("Other player connected. Your Move.");
	 	    	  deleteMessage(messages);
	 	    	  if(messages.get(0).getBody().equals("connected")) {
	 	    		  System.out.println("break from waiting for player2 loop");
	 	    		  break;
	 	    	  }
	 	      }
	 	  }
	   }
	   System.out.println("proceeding to game loop");
      SwingUtilities.invokeLater( 
         new Runnable() 
         {         
            public void run()
            {
               // display player's mark
            	System.out.println("Display into set text");
               idField.setText( "You are player \"" + myMark + "\"" );
            } // end method run
         } // end anonymous inner class
      ); // end call to SwingUtilities.invokeLater
      
      myTurn = ( myMark.equals( X_MARK ) ); // determine if client's turn

      // program the game logic below
      while ( ! isGameOver() )
      {
    	  List<Message> messages = new LinkedList<Message>();
    	  while(messages.isEmpty()) {
    		  if(gameOver)
    			  break;
    		  System.out.println("TRYING TO RECEIVE FROM: "+ QUEUE_RECEIVER);
    		  ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_RECEIVER);
    		  
    	      // Uncomment the following to provide the ReceiveRequestDeduplicationId
    	      //receiveMessageRequest.setReceiveRequestAttemptId("1");
    	      messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
    	      for (final Message message : messages) {
    	          System.out.println("Message from gameloop");
    	          System.out.println("  MessageId:     " + message.getMessageId());
    	          System.out.println("  ReceiptHandle: " + message.getReceiptHandle());
    	          System.out.println("  MD5OfBody:     " + message.getMD5OfBody());
    	          System.out.println("  Body:          " + message.getBody());
    	          for (final Entry<String, String> entry : message.getAttributes().entrySet()) {
    	              System.out.println("Attribute");
    	              System.out.println("  Name:  " + entry.getKey());
    	              System.out.println("  Value: " + entry.getValue());
    	          }
    	          //processMessage(message.getBody());
    	      }
    	      
    	  }
    	  if(gameOver)
    		  break;
    	  for (Message message : messages) {
    		  String tmp = message.getBody();
    		  System.out.println("Message received in game loop: "+tmp);
    		  deleteMessage(messages);
	          processMessage(tmp);
	          break;
	      }
    	  
    	      // Here in this while body, you will program the game logic. 
    	      // You are free to add any helper methods in this class or other classes.
    	      // Basically, this client player will retrieve a message from cloud in each while iteration
    	      // and process it until game over is detected.
    	      // Please check the processMessage() method below to gain some clues.
          
      } // end while
      
      
   } // end method run
   
   // You have write this method that checks the game board to detect winning status.
   private boolean isGameOver() {
	   System.out.println("Check if Game Over");
	   for(int i = 0; i < bsize; i++) {
		   for(int j = 0; j < bsize; j++) {
			   if(board[i][j].getMark().equals(myMark)) {
				   if(checkBelow(i, j, 1, myMark))
					   return true;
				   else if(checkAcross(i,j,1, myMark))
					   return true;
				   else if(checkRightDiagonal(i,j,1, myMark))
					   return true;
				   else if(checkLeftDiagonal(i,j,1, myMark))
					   return true;
			   }
		   }
	   }
	   System.out.println("Game not over");
	   return false;
   }
   private boolean checkLeftDiagonal(int i, int j, int count, String mark) {
	   if(offBoard(i+1, j-1)) {
		   return false;
	   }
	   else if(board[i+1][j-1].getMark().equals(myMark)) {
		   count++;
		   if(count ==4) {
			   board[i+1][j-1].setColorGreen();
			   return true;
		   } 
		   else {
			   if(checkLeftDiagonal(i+1, j-1, count, mark)) {
				   
				   board[i+1][j-1].setColorGreen();
				   return true;
			   }
		   }
	   }
	   return false;
   }
   private boolean checkRightDiagonal(int i, int j, int count, String mark) {
	   if(offBoard(i+1, j+1)) {
		   return false;
	   }
	   else if(board[i+1][j+1].getMark().equals(myMark)) {
		   count++;
		   if(count ==4) {
			   board[i+1][j+1].setColorGreen();
			   return true;
		   } 
		   else {
			   if(checkRightDiagonal(i+1, j+1, count, mark)) {
				   board[i+1][j+1].setColorGreen();
				   return true;
			   }
		   }
	   }
	   return false;
   }
   private boolean checkBelow(int i, int j, int count, String mark) {
	   if(offBoard(i+1, j)) {
		   return false;
	   }
	   else if(board[i+1][j].getMark().equals(myMark)) {
		   count++;
		   if(count ==4) {
			   board[i+1][j].setColorGreen();
			   return true;
		   } 
		   else {
			   if(checkBelow(i+1, j, count, mark)) {
				   
				   board[i+1][j].setColorGreen();
				   return true;
			   }
		   }
	   }
	   return false;
   }
   private boolean checkAcross(int i, int j, int count, String mark) {
	   if(offBoard(i, j+1)) {
		   return false;
	   }
	   else if(board[i][j+1].getMark().equals(myMark)) {
		   count++;
		   if(count ==4) {
			   board[i][j+1].setColorGreen();
			   return true;
		   } 
		   else {
			   if(checkAcross(i, j+1, count, mark)) {
				   
				   board[i][j+1].setColorGreen();
				   return true;
			   }
		   }
	   }
	   return false;
   }
   private boolean offBoard(int i, int j) {
	   if(i >= bsize || j>= bsize)
		   return true;
	   return false;
   }
   private void highlightGreen(String mark) {
	   for(int i = 0; i < bsize; i++) {
		   for(int j = 0; j < bsize; j++) {
			   if(board[i][j].getMark().equals(mark)) {
				   if(checkBelow(i, j, 1, mark))
					   return;
				   else if(checkAcross(i,j,1, mark))
					   return;
				   else if(checkRightDiagonal(i,j,1, mark))
					   return;
				   else if(checkLeftDiagonal(i,j,1, mark))
					   return;
			   }
		   }
	   }
   }
   // This method is not used currently, but it may give you some hints regarding
   // how one client talks to other client through cloud service(s).
   private void processMessage( String message )
   {
      // valid move occurred
	   if(gameOver)
		   return;
	   System.out.println("processing: "+message);
      if ( message.equals( "Opponent Won" ) ) 
      {
         displayMessage( "Game over, Opponent won.\n" );
         gameOver = true;
         int location = getOpponentMove();
         // then highlight the winning locations down below.
         placeMark(location);
         highlightGreen((myMark.equals(X_MARK) ? O_MARK : X_MARK));
         
      } // end if
      else if ( message.equals( "Opponent moved" ) ) 
      {
         int location = getOpponentMove(); // Here get move location from opponent
         								
         placeMark(location);
         myTurn = true; // now this client's turn
      } // end else if
      else
         displayMessage( message + "\n" ); // display the message
   } // end method processMessage

   private void placeMark(int location) {
	   int row = location / bsize; // calculate row
       int column = location % bsize; // calculate column
       System.out.println("Opponent moved to: "+location);
       setMark(  board[ row ][ column ], 
          ( myMark.equals( X_MARK ) ? O_MARK : X_MARK ) ); // mark move    
       if(!gameOver)
    	   displayMessage( "Opponent moved. Your turn.\n" );
   }
   //Here get move location from opponent
   private int getOpponentMove() {
	   ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_RECEIVER);

	      // Uncomment the following to provide the ReceiveRequestDeduplicationId
	      //receiveMessageRequest.setReceiveRequestAttemptId("1");
	      List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
	      for (final Message message : messages) {
	          int tmp = Integer.parseInt(message.getBody());
	          deleteMessage(messages);
	          return tmp;
	      }
	   return 0;
   }
   // manipulate outputArea in event-dispatch thread
   private void displayMessage( final String messageToDisplay )
   {
      SwingUtilities.invokeLater(
         new Runnable() 
         {
            public void run() 
            {
               displayArea.append( messageToDisplay ); // updates output
            } // end method run
         }  // end inner class
      ); // end call to SwingUtilities.invokeLater
   } // end method displayMessage

   // utility method to set mark on board in event-dispatch thread
   private void setMark( final Square squareToMark, final String mark )
   {
      SwingUtilities.invokeLater(
         new Runnable() 
         {
            public void run()
            {
               squareToMark.setMark( mark ); // set mark in square
            } // end method run
         } // end anonymous inner class
      ); // end call to SwingUtilities.invokeLater
   } // end method setMark

   // Send message to cloud service indicating clicked square
   public void sendClickedSquare( int location )
   {
      // if it is my turn
      if ( myTurn ) 
      {
         // Below you send the clicked location to the cloud service that will notify the opponent,
    	     // Or the opponent will retrieve the move location itself.
         // Please write your own code below.
    	  if(isGameOver()) {
    		  gameOver = true;
    		  sendMessage("Opponent Won");
    		  sendMessage(""+location);
    		  displayMessage("CONGRATULATIONS");
    		  highlightGreen((myMark.equals(X_MARK) ? X_MARK : O_MARK));
    		  
    	  }
    	  else {
    	  sendMessage("Opponent moved");
    	  sendMessage(""+location);
    	  displayMessage("Valid move, please wait.\n");
    	  }
         myTurn = false; // not my turn anymore
      } // end if
   } // end method sendClickedSquare

   // set current Square
   public void setCurrentSquare( Square square )
   {
      currentSquare = square; // set current square to argument
   } // end method setCurrentSquare

   // private inner class for the squares on the board
   private class Square extends JPanel 
   {
      private String mark; // mark to be drawn in this square
      private int location; // location of square
   
      public Square( String squareMark, int squareLocation )
      {
         mark = squareMark; // set mark for this square
         location = squareLocation; // set location of this square

         addMouseListener( 
            new MouseAdapter() {
               public void mouseReleased( MouseEvent e )
               {
            	   
                  setCurrentSquare( Square.this ); // set current square
                  if(isValidMove() && myTurn) {
                	  TicTacToeClient.this.setMark( currentSquare, myMark );
                	  displayMessage("You clicked at location: " + getSquareLocation() + "\n");
                	  sendClickedSquare(getSquareLocation());
                  }
                  
                  // You may have to send location of this square to 
                  // the cloud service that will notify the opponent player.
                  //if(isValidMove()) // you have write your own method isValidMove().
                        //sendClickedSquare( getSquareLocation() );
                  
               } // end method mouseReleased
            } // end anonymous inner class
         ); // end call to addMouseListener
      } // end Square constructor

      public boolean isValidMove() {
    	  if(currentSquare.getMark().equals(" ") && currentSquare.getSquareLocation() < bsize*bsize)
    		  return true;
    	  return false;
      }
      // return preferred size of Square
      public Dimension getPreferredSize() 
      { 
         return new Dimension( 30, 30 ); // return preferred size
      } // end method getPreferredSize

      // return minimum size of Square
      public Dimension getMinimumSize() 
      {
         return getPreferredSize(); // return preferred size
      } // end method getMinimumSize

      // set mark for Square
      public void setMark( String newMark ) 
      { 
         mark = newMark; // set mark of square
         repaint(); // repaint square
      } // end method setMark
      public String getMark() {
    	  return mark;
      }
      // return Square location
      public int getSquareLocation() 
      {
         return location; // return location of square
      } // end method getSquareLocation
   
      public void setColorGreen() {
    	  System.out.println("setting green at: "+location);
    	  getContentPane().setBackground(Color.GREEN);
      }
      // draw Square
      public void paintComponent( Graphics g )
      {
         super.paintComponent( g );
         
         g.drawRect( 0, 0, 29, 29 ); // draw square
         g.drawString( mark, 11, 20 ); // draw mark   
        
      } // end method paintComponent
   } // end inner-class Square
   
   

  public static void main( String args[] )
  {
     TicTacToeClient application; // declare client application

     // if no command line args
     if ( args.length == 0 )
        application = new TicTacToeClient( "" ); // 
     else
        application = new TicTacToeClient( args[ 0 ] ); // use args

     application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
  } // end main

} // end class TicTacToeClient


