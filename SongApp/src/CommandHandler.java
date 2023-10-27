import java.nio.file.Path;
import java.util.List;

/**Handles the logic of which command to execute. Designed with the goal of minimising the number selection statements in order to reduce cyclomatic complexity (cc).
 * This has replaced a lot of the code that was in takeCommand() (which itself had already been split into 3 quite large methods due to incredibly high cc)
 * This implementation has cleared up the code and improved readability significantly
 * The goal of minimal selection statements has been achieved as follows:
 * A string array containing all accepted inputs is searched through to see if user input is a valid command and the index of the command is saved.
 * That index is used to access an element from the Command interface array, each interface has an abstract method that has been set to execute one of the commands.
 * In effect, this allows me to create an array of methods which can be called using the interface array and their index. Something which is reminiscent of what is possible in the functional programming paradigm (An example would be Kotlin, which would have made me much happier if it was the language we used for this module)
 * Unfortunately, as Java is Object-Oriented, it doesn't allow methods to be treated as first-class objects. Hence, the heavy-handed approach I've had to take here.
 * I'm most likely going to move most of this code into Main.java because currently handleCommand() calls the functions by crossing into the Main.java file which is a not great way to do it. Moving it into Main.java will remove the requirement to cross files for the function calls
*/
public class CommandHandler {
    private final String[] acceptedInputs = {"all_songs", "plays_over", "add", "remove", "undo", "help", "history", "exit"};
    private final List<Song> lines;
    private final Path historyPath;
    private final List<String> historyLines;

    /**Applies values to attributes*/
    public CommandHandler(List<Song> lines, Path historyPath, List<String> historyLines) {
        this.lines = lines;
        this.historyPath = historyPath;
        this.historyLines = historyLines;
    }

    /**The Command interface serves as the first-class object vessel surrounding the abstract method that I alter with polymorphism*/
    private interface Command {
        //Empty abstract method to be overwritten
        List<Song> execute();
    }

    //interface array that stores the interfaces, each one with a unique method body
    private final Command[] methods = new Command[] {
            new Command() { public List<Song> execute() { Main.printSongs(lines, historyLines, historyPath); return null; } }, //printSongs
            new Command() { public List<Song> execute() { Main.playsOver(lines, historyLines, historyPath); return null; } }, //playsOver
            new Command() { public List<Song> execute() { return Main.add(lines, historyLines, historyPath); } }, //add
            new Command() { public List<Song> execute() { return Main.remove(lines, historyLines, historyPath); } }, //remove
            new Command() { public List<Song> execute() { return Main.undo(historyLines, historyPath);} } , //undo
            new Command() { public List<Song> execute() { Main.help(historyLines, historyPath); return null; } }, //help
            new Command() { public List<Song> execute() { Main.printList(historyLines); return null; } }, //printList
            () -> {  System.exit(0); return null; }, //exit
    };

    /**Searches through the accepted inputs to see if any match the input. Relevant method is then called from the methods array */
    public List<Song> handleCommand(String input) {
        for (int i = 0; i < acceptedInputs.length; i++) {
            if(input.equalsIgnoreCase(acceptedInputs[i])) return methods[i].execute(); //Executes relevant command if recognised
        }
        //Reached if input not accepted
        Main.unrecognisedCommand();
        return null;
    }
}
