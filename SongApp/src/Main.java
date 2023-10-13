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
"back" command - allows user to go back to the main interface if they have accidentally given an input and been taken to a secondary interface
"undo" command - undoes the last change to the file (Do this by storing the last state of the file and passing it to updateFile())
"history" command - shows the user the last x number of commands executed (Do this by storing a command history file of last x number of commands and updating it after every command)
If SongList.txt is not found in the project directory, it should display a message to the user to inform them that the file containing their saved songs could not be located and a new blank file has been generated for them
*/

public class Main {
    //Global Scanner variable
    //Wanted to avoid using global variables wherever possible but iterating over a local context containing Scanner definition and references leads to issues regarding input reading
    //Global Scanner also prevents the need for repeated declaration and de-allocation
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) throws IOException {
        //File path and file object instantiation
        Path path = Paths.get("SongList.txt");
        File file = new File(path.getFileName().toString());

        //Creates a file if it isn't already present
        //noinspection ResultOfMethodCallIgnored <--Translation for this: "Silence, IDE, trust me"
        file.createNewFile();

        //noinspection InfiniteLoopStatement <--The warning was annoying me, so I had to remove it
        while(true) { //I know while(true) is a bit naughty, but it works and doesn't cause any uncontrolled iteration as the loop awaits user input on every iteration
            //Read all lines
            List<Song> lines = getLines(path);

            takeCommand(lines, path);
        }
    }

    /**Takes in the user input and executes the appropriate block of code.
     * If command isn't recognised, it informs the user.
    */
    public static void takeCommand(List<Song> lines, Path path) {
        System.out.println("Main Menu");
        System.out.println("Type \"help\" for command list");
        System.out.print(">> "); //Shows the user where to type, aesthetic choice
        switch (sc.nextLine().toLowerCase()) { //Takes input and selects appropriate execution block
            case "all_songs":
                //Prints all the currently stored songs
                printSongs(lines);
                break;

            case "plays_over":
                //Prints all songs over specified play threshold
                playsOver(lines);
                break;

            case "add":
                //Adds a songs with specified details to the file
                lines = add(lines);
                break;

            case "remove":
                //Removes a specified song from the file
                lines = remove(lines); //I don't know why my IDE claims that this function returns the same value it takes when I can see that the contents change. It's probably because I'm returning the same data structure, and it just hasn't seen that an element has been removed.
                break;

            case "help":
                //Describes features to user
                System.out.println("all_songs - This command will show you all the songs you have currently stored");
                System.out.println("plays_over - This command allows you to narrow down your list of songs to only those that have at least a certain number of plays");
                System.out.println("add - This command allows you to add new songs into your stored list of songs. After entering this command, you will be asked for the details of the song");
                System.out.println("remove - This command allows you to remove songs from your stored list of songs. After entering this command, you will be asked for the name of the song");
                break;

            default:
                //Executes if user enters unrecognised command
                System.out.println("Sorry, I didn't recognise that command. Please ensure that everything is spelled as shown in the \"help\" menu");
                break;
        }
        try {updateFile(lines, path);}
        catch(IOException ignored){}
        System.out.println();
    }

    /**Reads all lines from the file and saves them to a Song list to be returned.
     * If input file is empty, an empty song list is returned.
    */
    public static List<Song> getLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Song> songList = new ArrayList<>();
        for (String line : lines) {
            songList.add(makeSongFromInput(line));
        }
        return songList;
    }

    /**Prints all the currently stored songs.
     * If no songs stored, prints message and returns.
     */
    public static void printSongs(List<Song> lines) {
        //Checks if any songs stored
        if(lines.isEmpty()) {
            System.out.println("No songs currently stored");
            return;
        }
        //Otherwise prints them all
        for (Song song : lines) {
            System.out.println(song.getName());
        }
    }

    /**Loops over songs and prints if above plays threshold.
     * Otherwise, displays message to user.
     */
    public static void printSongsOverNum(List<Song> lines, int minPlays) {
        boolean hasSongsOverMin = false; //OMG I WONDER WHAT THIS VARIABLE REPRESENTS
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
    public static void updateFile(List<Song> lines, Path path) throws IOException{
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

    /**Prints all songs over specified play threshold.*/
    public static void playsOver(List<Song> lines) {
        boolean isntInt;
        int num;
        //Loops until valid input
        do{
            num = 0;
            isntInt = false; // Used in validation process
            System.out.println("Please enter your desired minimum play count");
            System.out.print(">> ");
            String input = sc.nextLine();
            //Input validation
            try {num = Integer.parseInt(input);}
            catch (NumberFormatException e) {
                isntInt = true;
                System.out.println("Sorry, it appears you have entered an invalid number. Please ensure you enter a positive whole number");
            }
        } while(isntInt);
        printSongsOverNum(lines, num);
    }

    /**Adds a song with specified details to the file.*/
    public static List<Song> add(List<Song> lines) {
        boolean validInput; //For validation
        do { //Loops until valid input
            validInput = true;
            System.out.println("Enter song details in following format: name, artist, plays");
            System.out.print(">> ");
            String song = sc.nextLine();
            //Input Validation
            try {
                lines.add(makeSongFromInput(song));
            }
            catch (IOException e) {
                validInput = false;
                System.out.println("Sorry, it appears you have entered the details in the incorrect format. Please ensure that you have written it as shown in the example format");
            }
            catch (NumberFormatException e) {
                validInput = false;
                System.out.println("Sorry, it appears you have entered an invalid number for the play count. Please ensure you enter a positive whole number");
            }
        } while(!validInput);
        //Applies changes
        System.out.println("Song added");
        return lines;
    }

    /**Removes a specified song from the song list
     * If multiple songs with the same name have been added, it will remove only the first instance found in the file.
     * If specified song isn't found, it loops and re-prompts the user.
    */
    public static List<Song> remove(List<Song> lines) {
        boolean found = false; //Can you guess what needs to happen for this to become true?
        do { //Loops until valid input
            System.out.println("Enter song name");
            System.out.print(">> ");
            String line = sc.nextLine();
            //Loops through to see if any songs matching the input are stored, then removes it.
            for (Song thisSong : lines) {
                if (thisSong.getName().equals(line)) {
                    lines.remove(thisSong);
                    found = true; //Did you get it? Wow! That's very impressive.
                    break;
                }
            }
            if (!found) {
                System.out.println("Song not found");
            }
        } while(!found);
        //Applies changes
        System.out.println("Song removed");
        return lines;
    }
    
    /**Takes string input, splits it and sets the fields*/
    public static Song makeSongFromInput(String line) throws IOException, NumberFormatException {
        //Split into name, artist and plays
        //Rest of validation for this section is done at function call
        String[] details = line.split(", ");
        //Rest of validation for this section is done at function call
        if(details.length != 3) throw new IOException(); //Detects if user has inputted data incorrectly, I have decided to throw an IOException specifically since Exception is too broad and may lead to unintended exception catching
        int playCount = Integer.parseInt(details[2]);
        if(playCount < 0) throw new NumberFormatException(); // Ensuring the user entered a positive number (You can never trust the user)
        //Applies values
        return new Song(details[0], details[1], playCount);
        //If integer parsing fails, the NumberFormatException is caught and the user is notified of their severe lapse in judgement
    }
}