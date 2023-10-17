import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*Planned Development
"undo" command - undoes the last change to the file (Do this by storing the last state of the file and passing it to updateSongFile())
If SongList.txt is not found in the project directory, it should display a message to the user to inform them that the file containing their saved songs could not be located and a new blank file has been generated for them
*/

public class Main {
    //Global Scanner variable
    //Wanted to avoid using global variables wherever possible but iterating over a local context containing Scanner definition and references leads to issues regarding input reading
    //Global Scanner also prevents the need for repeated declaration and de-allocation
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) throws IOException {
        //File path and file object instantiation

        //Stores the songs currently held by the program
        Path songPath = Paths.get("SongList.txt");
        File songFile = new File(songPath.getFileName().toString());

        //Stores the history of the inputted commands
        Path historyPath = Paths.get("commandHistory.txt");
        File historyFile = new File(historyPath.getFileName().toString());

        //Creates a file if it isn't already present
        //If the file isn't already present, the user is notified that a new file has been created
        if(songFile.createNewFile()) System.out.println("--Notice-- Sorry, we weren't able to locate the song list file on your device. A new, blank file has been created for you");
        if(historyFile.createNewFile()) System.out.println("--Notice-- Sorry, we weren't able to locate the command history file on your device. A new, blank file has been created for you");


        //noinspection InfiniteLoopStatement <--The warning was annoying me, so I had to remove it
        while(true) { //I know while(true) is a bit naughty, but it works and doesn't cause any uncontrolled iteration as the loop awaits user input on every iteration
            //Read all lines from relevant files
            List<Song> lines = getSongLines(songPath);
            List<String> historyLines = Files.readAllLines(historyPath, StandardCharsets.UTF_8);
            takeCommand(lines, songPath, historyPath, historyLines);
        }
    }

    /**Takes in the user input and executes the appropriate block of code.
     * If command isn't recognised, it informs the user.
    */
    public static void takeCommand(List<Song> lines, Path songPath, Path historyPath, List<String> historyLines) {
        //User messages
        System.out.println("Main Menu");
        System.out.println("Type \"help\" for command list");
        System.out.print(">> "); //Shows the user where to type, aesthetic choice
        String input = sc.nextLine().toLowerCase();

        switch (input) { //Takes input and selects appropriate execution block
            case "all_songs":
                //Prints all the currently stored songs
                printSongs(lines, historyLines, historyPath);
                break;

            case "plays_over":
                //Prints all songs over specified play threshold
                playsOver(lines, historyLines, historyPath);
                break;

            case "help":
                //Describes features to user
                System.out.println("all_songs - This command will show you all the songs you have currently stored");
                System.out.println("plays_over - This command allows you to narrow down your list of songs to only those that have at least a certain number of plays");
                System.out.println("add - This command allows you to add new songs into your stored list of songs. After entering this command, you will be asked for the details of the song");
                System.out.println("remove - This command allows you to remove songs from your stored list of songs. After entering this command, you will be asked for the name of the song");
                System.out.println("history - This command will show you the last 10 commands that have been entered");
                updateHistoryFile("help", historyLines, historyPath);
                break;

            case "history":
                //Shows the previously inputted commands
                printList(historyLines);
                break;

            default:
                //I decided to break the switch statement into 2 pieces as the cyclomatic complexity scores of the combined switch statements exceed the generally agreed maximum at the method level
                checkIfFileEditingInput(input, lines, historyLines, historyPath);
                break;
        }
        //Apply changes to file
        try {updateSongFile(lines, songPath);}
        catch(IOException ignored){}
        System.out.println();
    }

    /**This method serves as the second half of the switch statement in takeCommand().
     * Contains the most cyclomatically complex commands (adding and removing songs)
     */
    public static void checkIfFileEditingInput(String input, List<Song> lines, List<String> historyLines, Path historyPath) {
        switch(input) {
            case "add":
                //Adds a songs with specified details to the file
                List<Song> addTemp = add(lines, historyLines, historyPath);
                if(addTemp == null) break; //add() returns null when user backs out
                lines = addTemp;
                break;

            case "remove":
                //Removes a specified song from the file
                List<Song> remTemp = remove(lines, historyLines, historyPath);
                if(remTemp == null) break; //remove() returns null when user backs out
                lines = remTemp; //Applying changes
                break;
            
            default:
            //Executes when user enters a nonexistent command
            System.out.println("Sorry, I didn't recognise that command. Please ensure that everything is spelled as shown in the \"help\" menu");
            break;
        }
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

    /**Saves lines back to specified file.*/
    public static void updateSongFile(List<Song> lines, Path path) throws IOException {
        //Makes FileWriter object
        FileWriter fw = new FileWriter(path.getFileName().toString());
        //Loops through lines and formats them to be saved to file
        for(int i = 0; i < lines.size(); i++) {
            String output = lines.get(i).getName() + ", " + lines.get(i).getArtist() + ", " + lines.get(i).getPlays();
            if(i != lines.size()-1) output += "\n"; //Added to ensure the last line doesn't have a return character at the end
            fw.write(output); //Adds to file
        }
        fw.close();
    }

    /**Adds a command to the command history file
     * Removes excess commands if the cutoff length has been exceeded
    */
    public static void updateHistoryFile(String command, List<String> lines, Path historyPath) {
        int historyListCutoffLength = 10; //Used to set how many of the most recent commands are stored at a time
        
        lines.add(command);

        //Loop to remove all commands over the cutoff length length
        while(lines.size() > historyListCutoffLength) lines.remove(0); //The reason of why I use a loop for this instead of a selection is there may be multiple lines too many if the files have been manually edited or if the program gets updated to use a shorter cutoff length
        
        //FileWriter needs a try or throws statement to be used to attempt file editing
        try (
        //Makes FileWriter object
        FileWriter fw = new FileWriter(historyPath.getFileName().toString())) {
            //Loops through lines and formats them to be saved to file
            for (int i = 0; i < lines.size(); i++) {
                String output = lines.get(i);
                if(i != lines.size() - 1) output += "\n"; //Added to ensure the last line doesn't have a return character at the end
                fw.write(output); //Adding to file
            }
            fw.close();
        }
        catch (IOException ignored) {}
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
            if(song.toLowerCase().equals("back")) return null; //Null value is returned and read, informing the program to not make any changes and to take a new command
            //Input Validation
            try {
                lines.add(makeSongFromInput(song));
            }
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

        updateHistoryFile(song, historyLines, historyPath);

        return lines;
    }

    /**Removes a specified song from the song list
     * If multiple songs with the same name have been added, it will remove only the first instance found in the file.
     * If specified song isn't found, it loops and re-prompts the user.
    */
    public static List<Song> remove(List<Song> lines, List<String> historyLines, Path historyPath) {
        boolean found = false; //Can you guess what needs to happen for this to become true?
        String line;
        List<Song> temp;
        do { //Loops until valid input
            System.out.println("Enter song name");
            System.out.println("Type \"back\" to return to the main menu");
            System.out.print(">> ");
            line = sc.nextLine();
            if(line.toLowerCase().equals("back")) return null; //Null value is returned and read, informing the program to not make any changes and to take a new command
            
            temp = removeSong(lines, line);
            found = (temp != null); //If temp is null, it means that removeSong() was unable to find the desired element and found is set to false
            if (!found) System.out.println("Song not found");
        } while(!found);
        //Applies changes
        System.out.println("Song removed");
        updateHistoryFile(line, historyLines, historyPath);
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
}