import java.io.IOException;

public class Song {
    //Song attributes set to private as implementation of encapsulation to prevent accidental editing and increase maintainability
    //Final as, after initialisation, they will have no requirement to change
    private final String name;
    private final String artist;
    private final int plays;

    /**Constructor takes string input, splits it and sets the attributes*/
    public Song(String line) throws IOException, NumberFormatException {
        //Split into name, artist and plays
        String[] details = line.split(", ");
        //Rest of validation for this section is done in the main class at instantiation
        if(details.length != 3) throw new IOException(); //Detects if user has inputted data incorrectly, I have decided to throw an IOException specifically since Exception is too broad and may lead to unintended exception catching
        //Applies values
        this.name = details[0];
        this.artist = details[1];
        this.plays = Integer.parseInt(details[2]); //If this fails, the NumberFormatException is caught and the user is notified of their severe lapse in judgement
    }

    //Getters
    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public int getPlays() {
        return plays;
    }
    //No setters as these values have no requirement to be changed
}