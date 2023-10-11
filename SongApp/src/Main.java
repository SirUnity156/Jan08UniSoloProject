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
    public static void main(String[] args) throws IOException {
        //File path and file object instantiation
        Path path = Paths.get("SongList.txt");
        File file = new File(path.getFileName().toString());

        //Creates a file if it isn't already present
        file.createNewFile();

        while(true) { //I know while(true) is a bit naughty, but it works and doesn't cause any uncontrolled iteration
            //Read all lines
            List<Song> lines = getLines(file, path);

            takeCommand(lines, path);
        }
    }
    public static void takeCommand(List<Song> lines, Path path) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Type \"help\" for command list");
        System.out.print(">> ");
        switch (sc.nextLine()) {
            case "all songs":
                printSongs(lines);
                break;

            case "plays over":
                playsOver(lines);
                break;

            case "add":
                add(lines, path);
                break;

            case "remove":
                remove(lines, path);
                break;

            case "help":
                System.out.println("all songs - provides a list of all songs currently stored");
                System.out.println("plays over - provides a list of all songs above a specified play count, will prompt you for minimum plays after command is entered");
                System.out.println("add - adds a new song to the song list, will prompt you for song details after command is entered");
                System.out.println("remove - removes a song from the song list, will prompt you for name of song to be removed after command is entered");
                break;

            default:
                System.out.println("Command not recognised");
                break;
        }
        sc.close();
        return;
    }
    public static List<Song> getLines(File file, Path path) throws IOException {
        //Reads all lines from the file and saves them to a Song list
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Song> songList = new ArrayList<Song>();
        if(!lines.isEmpty()) {
            for (String line : lines) {
                songList.add(new Song(line));
            }
        }
        return songList;
    }
    public static void printSongs(List<Song> lines) {
        //If no songs stored, prints message and returns
        if(lines.isEmpty()) {
            System.out.println("No songs currently stored");
            return;
        }
        //Otherwise prints them all
        for (Song song : lines) {
            System.out.println(song.getName());
        }
    }
    public static void printSongsOverNum(List<Song> lines, int minPlays) {
        boolean hasSongs = false; //OMG I WONDER WHAT THIS VARIABLE REPRESENTS
        //Loops over songs and prints if above plays threshold
        for (Song song: lines) {
            if(song.getPlays() > minPlays) {System.out.println(song.getName()); hasSongs = true;}
        }
        //Otherwise displays message
        if(!hasSongs) System.out.println("No songs over specified minimum plays");
    }
    public static void updateFile(List<Song> lines, Path path) throws IOException{
        //Makes FileWriter object
        FileWriter fw = new FileWriter(path.getFileName().toString());
        //Loops through lines and formats them to be saved to file
        for(int i = 0; i < lines.size(); i++) {
            String output = lines.get(i).getName() + " " + lines.get(i).getArtist() + " " + lines.get(i).getPlays();
            if(i != lines.size()-1) output += "\n";
            fw.write(output);
        }
        fw.close();
    }
    public static void playsOver(List<Song> lines) {
        Scanner sc = new Scanner(System.in);
        boolean isntInt;
        int num;
        //Loops until valid input
        do{
            num = 0;
            isntInt = false; // Used for validation
            System.out.println("Enter minimum plays");
            System.out.print(">>> ");
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
    public static void add(List<Song> lines, Path path) {
        Scanner sc = new Scanner(System.in);
        boolean validInput; //For validation
        do { //Loops until valid input
            validInput = true;
            System.out.println("Enter song details in following format: name, artist, plays");
            System.out.print(">>> ");
            String song = sc.nextLine();
            //Input Validation
            try {
                lines.add(new Song(song));
            }
            catch (Exception e) {
                validInput = false;
                System.out.println("Invalid input, ensure correct formatting");
            }
        } while(!validInput);
        //Applies changes
        try {updateFile(lines, path);}
        catch(IOException ignored){}
        System.out.println("Song added");
    }
    public static void remove(List<Song> lines, Path path) {
        Scanner sc = new Scanner(System.in);
        boolean found = false; //Can you guess what needs to happen for this to become true?
        do { //Loops until valid input
            System.out.println("Enter song name");
            System.out.print(">>> ");
            String line = sc.nextLine();
            //Loops through to see if any songs matching the input are stored, then removes it.
            //If, for some reason, you have added multiple songs with the same name, it will remove only the first instance found in the file
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
}