import re
import sys

def parse_tja_file(input_file, output_file):
    # Initialize variables
    path_to_mp3 = None
    genre = None
    bpm = None
    difficulty_normal = None
    note_data = []
    current_time = 0.0  # Track time in milliseconds

    # Load the TJA file
    with open(input_file, "r", encoding="utf-8") as file:
        tja_content = file.readlines()

    # Time conversion helper (millis to MM:SS:millis)
    def millis_to_timecode(millis):
        minutes = int(millis // 60000)
        seconds = int((millis % 60000) // 1000)
        millis = int(millis % 1000)
        return f"{minutes:02}:{seconds:02}:{millis:03}"

    # Process the TJA file line by line
    for line in tja_content:
        line = line.strip()

        # Extract metadata
        if line.startswith("WAVE:"):
            path_to_mp3 = line.split(":", 1)[1].strip()
        elif line.startswith("GENRE:"):
            genre = line.split(":", 1)[1].strip()
        elif line.startswith("BPM:"):
            bpm = float(line.split(":", 1)[1].strip())
        elif line.startswith("COURSE: Normal") or line.startswith("COURSE: 1"):
            difficulty_normal = None  # Prepare to extract LEVEL value
        elif line.startswith("LEVEL:") and difficulty_normal is None:
            difficulty_normal = line.split(":", 1)[1].strip()

        # Process notes
        elif re.match(r"^[0-9]+,", line):  # Matches measure lines
            notes = re.findall(r"[0123456789]", line)  # Extract notes

            if bpm is not None:  # Ensure BPM is set
                note_interval = (60000 / bpm) / len(notes)  # Milliseconds per note

                for note in notes:
                    if note == "1":  # Don
                        note_data.append(f"{millis_to_timecode(current_time)} 1")
                    elif note == "2":  # Ka
                        note_data.append(f"{millis_to_timecode(current_time)} 0")

                    current_time += note_interval  # Increment time

    # Format the output
    output_data = f"""
PATH_TO_MP3_FILE: {path_to_mp3}
GENRE: {genre}
DIFFICULTY: {difficulty_normal}
BPM: {bpm}

Note Timing:
""" + "\n".join(note_data)

    # Save to output file
    with open(output_file, "w", encoding="utf-8") as out_file:
        out_file.write(output_data)

    print(f"Extraction complete. Output saved to {output_file}")

# Run the script with command-line arguments
if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python tja_parser.py <input_file.tja> <output_file.txt>")
    else:
        parse_tja_file(sys.argv[1], sys.argv[2])
