public class Note {
    public long hitTime;  // When the note should hit (ms from song start)
    public int type;      // 0 = small Don, 1 = small Ka, 2 = big Don, 3 = big Ka
    public int x;         // Current x position
    // For big notes, track which halves have been hit.
    public boolean leftHit = false;
    public boolean rightHit = false;
    public long leftError = Long.MAX_VALUE;
    public long rightError = Long.MAX_VALUE;

    // Constructor takes the hit time, type, and starting x position.
    public Note(long hitTime, int type, int startX) {
        this.hitTime = hitTime;
        this.type = type;
        this.x = startX;
    }
}
