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

public class Main {
    //Global Scanner variable
    //Wanted to avoid using global variables wherever possible but iterating over a local context containing a Scanner definition and references leads to issues regarding input reading
    //Global Scanner also prevents the need for repeated declaration and de-allocation lines
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) throws IOException {
        //File path and file object instantiation
        Path path = Paths.get("SongList.txt");
        File file = new File(path.getFileName().toString());

        //Creates a file if it isn't already present
        file.createNewFile();

        while(true) { //I know while(true) is a bit naughty, but it works and doesn't cause any uncontrolled iteration as the loop awaits user input on every iteration
            //Read all lines
            List<Song> lines = getLines(file, path);

            takeCommand(lines, path);
        }
    }

    /**Takes in the user input and executes the appopriate block of code.
     * If command isn't recognised, it informs the user.
    */
    public static void takeCommand(List<Song> lines, Path path) {
        System.out.println("Type \"help\" for command list");
        System.out.print(">> "); //Shows the user where to type, aesthetic choice
        String input = sc.nextLine();
        switch (input) { //Takes input and selects appropriate execution block
            case "all songs":
                //Prints all the currently stored songs
                printSongs(lines);
                break;

            case "plays over":
                //Prints all songs over specified play threshold
                playsOver(lines);
                break;

            case "add":
                //Adds a songs with specified details to the file
                add(lines, path);
                break;

            case "remove":
                //Removes a specified song from the file
                remove(lines, path);
                break;

            case "help":
                //Describes features to user
                System.out.println("all songs - provides a list of all songs currently stored");
                System.out.println("plays over - provides a list of all songs above a specified play count, will prompt you for minimum plays after command is entered");
                System.out.println("add - adds a new song to the song list, will prompt you for song details after command is entered");
                System.out.println("remove - removes a song from the song list, will prompt you for name of song to be removed after command is entered");
                break;

            default:
                //Executes if user enters unrecognised command
                System.out.println("Command not recognised");
                break;
        }
    }

    /**Reads all lines from the file and saves them to a Song list to be returned.
     * If input file is empty, an empty song list is returned.
    */
    public static List<Song> getLines(File file, Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Song> songList = new ArrayList<Song>();
        if(!lines.isEmpty()) {
            for (String line : lines) {
                songList.add(makeSongFromInput(line));
            }
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
     * Otherwise displays message to user.
     */
    public static void printSongsOverNum(List<Song> lines, int minPlays) {
        boolean hasSongsOverMin = false; //OMG I WONDER WHAT THIS VARIABLE REPRESENTS
        for (Song song: lines) {
            //Iterates through and checks if song has sufficient plays
            if(song.getPlays() > minPlays) {System.out.println(song.getName()); hasSongsOverMin = true;}
        }
        //Message for user if no matches
        if(!hasSongsOverMin) System.out.println("No songs over specified minimum plays");
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
            isntInt = false; // Used for validation
            System.out.println("Enter minimum plays");
            System.out.print(">> ");
            String input = sc.nextLine();
            //Input validation
            try {num = Integer.parseInt(input);}
            catch (NumberFormatException e) {
                isntInt = true;
                System.out.println("Invalid input, enter an integer");
            }
        } while(isntInt);
        printSongsOverNum(lines, num);
    }

    /**Adds a song with specified details to the file.*/
    public static void add(List<Song> lines, Path path) {
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
                System.out.println("Invalid input, ensure correct formatting");
            }
            catch (NumberFormatException e) {
                validInput = false;
                System.out.println("Inalid input, Ensure play count is an integer");
            }
        } while(!validInput);
        //Applies changes
        try {updateFile(lines, path);}
        catch(IOException ignored){}
        System.out.println("Song added");
    }

    /**Removes a specified song from the song list
     * If multiple songs with the same name have been added, it will remove only the first instance found in the file.
     * If specified song isn't found, it loops and reprompts the user.
    */
    public static void remove(List<Song> lines, Path path) {
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
        try {updateFile(lines, path);}
        catch(IOException ignored){}
        System.out.println("Song removed");
    }
    
    /**Takes string input, splits it and sets the fields*/
    public static Song makeSongFromInput(String line) throws IOException, NumberFormatException {
        //Split into name, artist and plays
        //Rest of validation for this section is done at function call
        String[] details = line.split(", ");
        //Rest of validation for this section is done at function call
        if(details.length != 3) throw new IOException(); //Detects if user has inputted data incorrectly, I have decided to throw an IOException specifically since Exception is too broad and may lead to unintended exception catching
        //Applies values
        return new Song(details[0], details[1], Integer.parseInt(details[2]));
        //If integer parsing fails, the NumberFormatException is caught and the user is notified of their severe lapse in judgement
    }
}