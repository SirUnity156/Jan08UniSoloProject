import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/*Planned Development
- "redo" command - reverses the changes made by undo (do this by changing how undo() works such that undone states are not deleted and the current position on the state timeline should be stored in the file
- Cut down the number of parameters requested by a method by getting file states within each method rather than passing them in
- "Debug" dev command showing all previous takeCommand completion number
- Create generic "updateFile" function to prevent code repetition in updateSongFile, updateHistoryFile & updateDebugFile
*/

public class Main {
    //Global Scanner variable
    //Wanted to avoid using global variables wherever possible but iterating over a local context containing Scanner definition and references leads to issues regarding the input reading
    //Global Scanner also prevents the need for repeated declaration and de-allocation or passing into numerous local contexts
    static Scanner sc = new Scanner(System.in);

    //Global variable to store the previous states of the list for every time a change occurs
    //I have elected to store the previous states internally within the program rather than in an external file so that all data of previous states are lost. This prevents users from being able to undo changes made in previous instances of the program, thereby eliminating a source of user confusion/privacy breach
    //Once again, as with global Scanner, the purpose of this variable being global is to reduce the quantity of arguments required to be passed into the local contexts
    static List<List<Song>> previousStates = new ArrayList<>(0);

    public static void main(String[] args) throws IOException {

        //Shutdown hook to display a message when the program closes for any reason.
        //This means that users will be able to see a message whether they use the exit command or just close the terminal/JVM itself.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down..."); // Code to run on exit
        }));

        //File path and file object instantiation

        //Stores the songs currently held by the program
        Path songPath = Paths.get("SongList.txt");
        File songFile = new File(songPath.getFileName().toString());

        //Stores the history of the inputted commands
        Path historyPath = Paths.get("commandHistory.txt");
        File historyFile = new File(historyPath.getFileName().toString());

        //Stores the history of completion codes
        Path debugPath = Paths.get("debug.txt");
        File debugFile = new File(debugPath.getFileName().toString());

        //Creates a file if it isn't already present
        //If the file isn't already present, the user is notified that a new file has been created
        if(songFile.createNewFile()) System.out.println("--Notice-- Sorry, we weren't able to locate the song list file on your device. A new file has been created for you");
        if(historyFile.createNewFile()) System.out.println("--Notice-- Sorry, we weren't able to locate the command history file on your device. A new file has been created for you");
        //noinspection ResultOfMethodCallIgnored
        debugFile.createNewFile(); //Ensure existence of debugFile

        //This will be used in the debug log to track types of command completions happening in the code (exits, no file change, file change etc.) for aid in development and debugging
        int completionCode;

        //Loops until the completion code is 0 (resulting from user exit)
        do {
            //Read all lines from relevant files
            List<Song> lines = getSongLines(songPath);
            List<String> historyLines = Files.readAllLines(historyPath, StandardCharsets.UTF_8);

            //Saves completion code for debug log
            /*
             * 0 - exit
             * 1 - completed with no file change
             * 2 - completed with file change
             */
            completionCode = takeCommand(lines, songPath, historyPath, historyLines);
            updateDebugFile(completionCode, debugPath);

        } while(completionCode != 0);
    }

    /**Takes in the user input and consults a CommandHandler object to execute the appropriate block of code.
     * If command isn't recognised, it informs the user.
     * Integer return value represents the completion code
    */
    public static int takeCommand(List<Song> lines, Path songPath, Path historyPath, List<String> historyLines) throws IOException {
        //User messages
        System.out.println();
        System.out.println("Main Menu");
        System.out.println("Type \"help\" for command list");
        System.out.print(">> "); //Shows the user where to type, aesthetic choice
        String input = sc.nextLine().toLowerCase();

        //Saves the state of the file before the command is executed
        int tempPrevStateLen = previousStates.size();
        List<Song> prevState = listAssignWithoutReference(lines);

        //CommandHandler object taking input and directing the call to the right method and returning the state of the song list after command execution
        List<Song> newLines = (new CommandHandler(lines, historyPath, historyLines, songPath).handleCommand(input));

        if(newLines == null) return 1; //A return value of null means that no changes have been made and the file does not need to be updated
        if(newLines.size() == 1 && newLines.get(0).getPlays() == -1) return 0; //User has entered the exit command and the command will close
        
        //Update previous states if a change has happened
        if(previousStates.size() == tempPrevStateLen && newLines != prevState) updatePreviousStates(prevState);
        
        //Apply changes to file
        updateSongFile(newLines, songPath);
        return 2; //Completed with file update
    }

    /**Allows user to update details of songs that are already stored
     * Asks for song's current name and will loop until valid input or user backing out
     * Then asks for new details, loop until valid input or user backing out
     * Applies and returns values
    */
    public static List<Song> update(Path path) throws IOException{
        //Reading lines from the file
        List<Song> lines = getSongLines(path);
        String line;
        int index;
        do { //Loops until valid input
            System.out.println("Enter the current name of the song that you wish to update");
            System.out.println("Type \"back\" to return to the main menu");
            System.out.print(">> ");
            line = sc.nextLine();

            if(line.equalsIgnoreCase("back")) return null; //Null value is returned and read, informing the program to not make any changes and to take a new command
            index = findLineByName(lines, line);
            if (index == -1) System.out.println("Song not found");
        } while(index == -1);
        System.out.println("Song found!");
        return updateInputLoop(lines, index);
    }

    /**Used as part of update method to find the index*/
    public static int findLineByName(List<Song> lines, String line) {
        for (int i = 0; i < lines.size(); i++) {
            if(Objects.equals(lines.get(i).getName(), line)) return i;
        }
        //Returns -1 if index not found
        return -1;
    }

    /**Handles the input validation for entering the song details for updating */
    public static List<Song> updateInputLoop(List<Song> lines, int index) {
        boolean validInput; //Flag marking validation status
        String song; // input
        Song songSong = new Song("", "", 0);
        do { //Loops until valid input
            validInput = true;
            System.out.println("Enter the new details for the song in following format: name, artist, plays");
            System.out.println("Type \"back\" to return to the main menu");
            System.out.print(">> ");
            song = sc.nextLine();
            if(song.equalsIgnoreCase("back")) return null; //Null value is returned and read, informing the program to not make any changes and to take a new command
            //Input Validation
            try {songSong = makeSongFromInput(song);}
            catch (IOException e) {
                //Executes if the format doesn't match the expected format
                validInput = false;
                System.out.println("Sorry, it appears you have entered the details in the incorrect format. Please ensure that you have written it as shown in the example format");
            }
            catch (NumberFormatException e) {
                //Executes if the user doesn't give a valid value for the play count
                validInput = false;
                System.out.println("Sorry, it appears you have entered an invalid number for the play count. Please ensure you enter a positive whole number");
            }
        } while(!validInput);
        //Applying and returning values
        lines.set(index, songSong);
        return lines;
    }

    /**Shows and describes all accepted commands to the user */
    public static void help(List<String> historyLines, Path historyPath) {
        System.out.println("all_songs - This command will show you all the songs you have currently stored");
        System.out.println("plays_over - This command allows you to narrow down your list of songs to only those that have at least a certain number of plays");
        System.out.println("add - This command allows you to add new songs into your stored list of songs. After entering this command, you will be asked for the details of the song");
        System.out.println("remove - This command allows you to remove songs from your stored list of songs. After entering this command, you will be asked for the name of the song");
        System.out.println("history - This command will show you the last 10 commands that have been entered (Oldest to newest)");
        System.out.println("undo - This command will allow you to undo changes you have made to the song file, please not that you cannot undo changes from previous instances of the application");
        System.out.println("update - This command will allow you to update the details of songs already stored in the application");
        updateHistoryFile("help", historyLines, historyPath);
    }

    /**Executes if the user enters an unrecognised command */
    public static void unrecognisedCommand() {
        System.out.println("Sorry, I didn't recognise that command. Please ensure that everything is spelled as shown in the \"help\" menu");
    }

    /**Reads all lines from the file and saves them to a Song list to be returned.
     * If input file is empty, an empty song list is returned.
    */
    public static List<Song> getSongLines(Path path) throws IOException {
        //Reading all the files lines into a list
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Song> songList = new ArrayList<>();
        for (String line : lines) {
            //Formatting file lines into a Song list
            songList.add(makeSongFromInput(line));
        }
        return songList;
    }

    /**Prints all the currently stored songs.
     * If no songs stored, prints message and returns.
     */
    public static void printSongs(List<Song> lines, List<String> historyLines, Path historyPath) {
        //Checks if any songs stored
        if(lines.isEmpty()) {
            System.out.println("No songs currently stored");
            return;
        }
        //Otherwise prints them all
        for (Song song : lines) {
            System.out.println(song.getName());
        }
        updateHistoryFile("all_songs", historyLines, historyPath);
    }

    /**Loops over songs and prints if above plays threshold.
     * Otherwise, displays message to user.
     */
    public static void printSongsOverNum(List<Song> lines, int minPlays) {
        boolean hasSongsOverMin = false; //Used to know whether if any applicable songs exist
        for (Song song: lines) {
            //Iterates through and checks if song has sufficient plays
            if(song.getPlays() > minPlays) {
                System.out.println(song.getName());
                hasSongsOverMin = true;
            }
        }
        //Message for user if no matches
        if(!hasSongsOverMin) System.out.println("Sorry, there are no songs stored above your desired minimum plays");
    }

    /**Saves lines back to specified file*/
    public static void updateSongFile(List<Song> lines, Path path) throws IOException {
        //Makes FileWriter object
        FileWriter fw = new FileWriter(path.getFileName().toString());
        //Loops through lines and formats them to be saved to file
        for(int i = 0; i < lines.size(); i++) {
            StringBuilder sBuilder = new StringBuilder(lines.get(i).getName() + ", " + lines.get(i).getArtist() + ", " + lines.get(i).getPlays());
            if(i != lines.size()-1) sBuilder.append("\n"); //Added to ensure the last line doesn't have a return character at the end
            fw.write(sBuilder.toString()); //Adds to file
        }
        fw.close();
    }

    /**Adds a command to the command history file
     * Removes excess commands if the cutoff length has been exceeded
    */
    public static void updateHistoryFile(String command, List<String> lines, Path historyPath) {
        int historyListCutoffLength = 10; //Used to set how many of the most recent commands are stored at a time

        lines.add(command);

        //Loop to remove all commands over the cutoff  length
        while(lines.size() > historyListCutoffLength) lines.remove(0);
        //The reason of why I use a loop for this instead of a selection is there may be multiple lines too many if the files have been manually edited or if the program gets updated to use a shorter cutoff length
        
        //FileWriter needs a try or throws statement to be used to attempt file editing
        try (
        //Makes FileWriter object
        FileWriter fw = new FileWriter(historyPath.getFileName().toString())) {
            //Loops through lines and formats them to be saved to file
            for (int i = 0; i < lines.size(); i++) {
                StringBuilder sBuilder = new StringBuilder(lines.get(i));
                if(i != lines.size() - 1) sBuilder.append("\n"); //Added to ensure the last line doesn't have a return character at the end
                fw.write(sBuilder.toString()); //Adding to file
            }
        }
        catch(IOException e){System.out.println(e.getMessage());}
    }

    /**Adds the most recent completion code to the debug file*/
    public static void updateDebugFile(int completionCode, Path debugPath) throws IOException {
        int debugListCutoffLength = 100; //Used to set how many of the most recent completion codes are stored at a time

        //Gets lines and adds new line
        List<String> debugLines = Files.readAllLines(debugPath, StandardCharsets.UTF_8);
        debugLines.add(String.valueOf(completionCode));

        //Loop to remove all commands over the cutoff length
        while(debugLines.size() > debugListCutoffLength) debugLines.remove(0);

        //Formats line and writes to file
        StringBuilder sb = new StringBuilder();
        for(String line : debugLines) {
            sb.append(line);
            sb.append("\n");
        }
        FileWriter fw = new FileWriter(debugPath.getFileName().toString());
        fw.write(sb.toString());
        fw.close();
    }

    /**Adds a new state to the previous state list and shortens it if the list is too long*/
    public static void updatePreviousStates(List<Song> lines) {
        int undoMemoryCutoff = 10; //Used to add a maximum number of possible state storing to prevent unnecessary memory usage
        previousStates.add(lines);
        while(previousStates.size() > undoMemoryCutoff) previousStates.remove(0); //Loops to remove the oldest states if too many states are stored
    }

    /**Prints all songs over specified play threshold.*/
    public static void playsOver(List<Song> lines, List<String> historyLines, Path historyPath) {
        boolean isntInt; //Used for input validation
        int minimum; //Represents the minimum number of plays a song is required to have to be included in the search result0
        //Loops until valid input
        do{
            minimum = 0;
            isntInt = false; // Used in validation process
            //User messages
            System.out.println("Please enter your desired minimum play count");
            System.out.println("Type \"back\" to return to the main menu");
            System.out.print(">> ");
            String input = sc.nextLine();
            if(input.equals("back")) return; //This ends the procedure early and returns to the main menu
            //Input validation
            try {minimum = Integer.parseInt(input);}
            catch (NumberFormatException e) {
                isntInt = true;
                System.out.println("Sorry, it appears you have entered an invalid number. Please ensure you enter a positive whole number");
            }
        } while(isntInt);
        printSongsOverNum(lines, minimum);
        updateHistoryFile("plays_over " + minimum, historyLines, historyPath);
    }

    /**Adds a song with specified details to the file.*/
    public static List<Song> add(List<Song> lines, List<String> historyLines, Path historyPath) {
        boolean validInput; //For validation
        String song;
        do { //Loops until valid input
            validInput = true;
            System.out.println("Enter song details in following format: name, artist, plays");
            System.out.println("Type \"back\" to return to the main menu");
            System.out.print(">> ");
            song = sc.nextLine();
            if(song.equalsIgnoreCase("back")) return null; //Null value is returned and read, informing the program to not make any changes and to take a new command
            //Input Validation
            try {lines.add(makeSongFromInput(song));}
            catch (IOException e) {
                //Executes if the format doesn't match the expected format
                validInput = false;
                System.out.println("Sorry, it appears you have entered the details in the incorrect format. Please ensure that you have written it as shown in the example format");
            }
            catch (NumberFormatException e) {
                //Executes if the user doesn't give a valid value for the play count
                validInput = false;
                System.out.println("Sorry, it appears you have entered an invalid number for the play count. Please ensure you enter a positive whole number");
            }
        } while(!validInput);
        //Applies changes
        System.out.println("Song added");

        updateHistoryFile("add " + song, historyLines, historyPath);
        return lines;
    }

    /**Removes a specified song from the song list
     * If multiple songs with the same name have been added, it will remove only the first instance found in the file.
     * If specified song isn't found, it loops and re-prompts the user.
    */
    public static List<Song> remove(List<Song> lines, List<String> historyLines, Path historyPath) {
        boolean found; //Can you guess what needs to happen for this to become true?
        String line;
        List<Song> temp;
        do { //Loops until valid input
            System.out.println("Enter song name");
            System.out.println("Type \"back\" to return to the main menu");
            System.out.print(">> ");
            line = sc.nextLine();
            if(line.equalsIgnoreCase("back")) return null; //Null value is returned and read, informing the program to not make any changes and to take a new command
            
            temp = removeSong(lines, line);
            found = (temp != null); //If temp is null, it means that removeSong() was unable to find the desired element and found is set to false
            if (!found) System.out.println("Song not found");
        } while(!found);
        //Applies changes
        System.out.println("Song removed");
        updateHistoryFile("remove " + line, historyLines, historyPath);
        return lines;
    }
    
    /**Removes a song from the list
     * Returns null if song not found
     * Otherwise returns updated list
     * This block of code was originally contained within remove() but I elected to abstract it into its own method to decrease cyclomatic complexity and remove a "bumpy road" section of my code
    */
    public static List<Song> removeSong(List<Song> lines, String line) {
        //Loops through to see if any songs matching the input are stored, then removes it.
        for (Song thisSong : lines) {
            if (thisSong.getName().equals(line)) {
                //When the song is found, it's then removed and the new list is returned
                lines.remove(thisSong);
                return lines;
            }
        }
        //Returns null if element not found
        return null;
    }

    /**Takes string input, splits it and sets the fields*/
    public static Song makeSongFromInput(String line) throws IOException, NumberFormatException {
        //Split into name, artist and plays
        String[] details = line.split(", ");
        //Rest of validation for this section is done at function call
        if(details.length != 3) throw new IOException(); //Detects if user has inputted data incorrectly, I have decided to throw an IOException specifically since Exception is too broad and may lead to unintended exception catching
        int playCount = Integer.parseInt(details[2]);
        if(playCount < 0) throw new NumberFormatException(); // Ensuring the user entered a positive number (You can never trust the user)
        //Applies values
        return new Song(details[0], details[1], playCount);
        //If integer parsing fails, the NumberFormatException is caught in the surrounding context and the user is notified of their severe lapse in judgement
    }

    /**Prints the contents of a list
     * Taken into its own method to reduce cyclomatic complexity
    */
    public static void printList(List<String> list) {
        for (String element : list) {
            System.out.println(element);
        }
    }

    /**Undoes the most recent change to the song list */
    public static List<Song> undo(List<String> historyLines, Path historyPath) {
        if(previousStates.isEmpty()) { //Detects if there are no stored changes
            System.out.println("Sorry, no changes have been recorded yet in this instance of the application");
            return null; //null return data is picked up after function call and interpreted accordingly
        }
        List<Song> temp = previousStates.get(previousStates.size()-1); //Used to store desired file contents state
        previousStates.remove(previousStates.size()-1); //Removes the state that was just undone (might reverse this in future to allow for redo feature)
        updateHistoryFile("undo", historyLines, historyPath);
        return temp;
    }

    /**Used to copy values from one list to another.
     * The reason that I don't just write newList = oldList is because that operation doesn't copy the values across to the new list object.
     * Instead, it sends the original object reference and any changes to the new list will also occur to the old list
     * This caused me significant mental anguish before I figured it out
     */
    public static <T> List<T> listAssignWithoutReference(List<T> toBeAssigned) {
        List<T> newList = new ArrayList<>(0);
        newList.addAll(toBeAssigned);
        return newList;
    }
}