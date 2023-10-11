public class Song {
    //Song attributes set to private as implementation of encapsulation to prevent accidental editing and increase maintainability
    //Final as, after initialisation, they will have no requirement to change
    private final String name;
    private final String artist;
    private final int plays;

    /**Constructor takes string input, splits it and sets the attributes*/
    public Song(String line) {
        //Split into name, artist and plays
        String[] deets = line.split(", ");
        //Validation for this section is done in the main class at instantiation
        //Applies values
        this.name = deets[0];
        this.artist = deets[1];
        this.plays = Integer.parseInt(deets[2]);
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
