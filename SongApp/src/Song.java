public class Song {
    //Song fields set to private as implementation of encapsulation to prevent accidental editing and increase maintainability
    //Final as, after initialisation, they will have no requirement to change
    private final String name;
    private final String artist;
    private final int plays;

    /**Constructor takes input values and sets the fields*/
    public Song(String name, String artist, int plays) {
        this.name = name;
        this.artist = artist;
        this.plays = plays;
    }

    //Getters
    /**Returns name */
    public String getName() {
        return name;
    }

    /**Returns artist */
    public String getArtist() {
        return artist;
    }

    /**Returns plays */
    public int getPlays() {
        return plays;
    }
    //No setters as these values have no requirement to be changed
}