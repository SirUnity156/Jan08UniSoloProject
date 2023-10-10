public class Song {
    //Attributes as requested in requirements
    private String name;
    private String artist;
    private int plays;
    public Song(String line) {
        //Constructor splits the line into its parts
        String[] deets = line.split(", ");
        //Validation for this section is done in the main class at instantiation
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
}
