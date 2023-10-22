import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SongCommandHandler {
    private final String[] acceptedInputs = {"all_songs", "plays_over", "add", "remove", "undo", "help", "history", "exit"};
    private List<Song> lines;
    private final Path songPath;
    private final Path historyPath;
    private final List<String> historyLines;

    public SongCommandHandler(List<Song> lines, Path songPath, Path historyPath, List<String> historyLines) {
        this.lines = lines;
        this.songPath = songPath;
        this.historyPath = historyPath;
        this.historyLines = historyLines;
    }

    private interface Command {
         List<Song> execute();
    }
    private final Command[] methods = new Command[] {
            new Command() { public List<Song> execute() { Main.printSongs(lines, historyLines, historyPath); return null; } },
            new Command() { public List<Song> execute() { Main.playsOver(lines, historyLines, historyPath); return null; } },
            new Command() { public List<Song> execute() { return Main.add(lines, historyLines, historyPath); } },
            new Command() { public List<Song> execute() { return Main.remove(lines, historyLines, historyPath); } },
            new Command() { public List<Song> execute() { return Main.undo();} } ,
            new Command() { public List<Song> execute() { Main.help(historyLines, historyPath); return null; } },
            new Command() { public List<Song> execute() { Main.printList(historyLines); return null; } },
            () -> {  System.exit(0); return null; },
    };

    public List<Song> handleCommand(String input) {
        input = input.toLowerCase();
        for (int i = 0; i < acceptedInputs.length; i++) {
            if(input.equals(acceptedInputs[i])) {
                return methods[i].execute();
            }
        }
        Main.unrecognisedCommand();
        return null;
    }
}