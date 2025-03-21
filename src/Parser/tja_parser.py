import argparse

def format_timestamp(ms):
    """Convert milliseconds to a MM:SS:ms formatted string."""
    minutes = ms // 60000
    seconds = (ms % 60000) // 1000
    millis = ms % 1000
    return f"{minutes:02d}:{seconds:02d}:{millis:03d}"

def convert_tja_to_bin(tja_path, bin_path, target_difficulty):
    """
    Convert a .tja chart file to .bin format for the Taiko simulator using only four note types.
    
    Note Mapping:
      TJA '1' -> Little Don  -> .bin 0
      TJA '2' -> Little Ka  -> .bin 1
      TJA '3' -> Big Don    -> .bin 2
      TJA '4' -> Big Ka     -> .bin 3

    The output file is formatted with a header containing:
      PATH_TO_MP3_FILE, GENRE, DIFFICULTY, and BPM,
    followed by a blank line, then "Note Timing:" and a list of note timings
    formatted as MM:SS:ms note_value.
    """
    # Read the TJA file (assuming UTF-8 encoding)
    with open(tja_path, 'r', encoding='utf-8-sig') as f:
        lines = f.readlines()
    
    # --- Global Metadata Extraction ---
    mp3_path = ""
    genre = ""
    base_bpm = 120.0  # Default BPM
    offset_sec = 0.0
    
    # Parsing state variables
    parsing_target = False   # True when inside the target course section
    in_chart = False         # True when inside a #START ... #END block
    difficulty_str = target_difficulty  # Use provided difficulty by default
    current_bpm = None
    measure_numer = 4.0      # Default time signature numerator
    measure_denom = 4.0      # Default time signature denominator
    
    # Timing and note accumulation
    current_time = 0.0
    measure_start_bpm = None
    notes_buffer = []        # Holds note symbols (as integers) for the current measure
    bpm_events = []          # Holds BPM change events as (index, new_bpm)
    output_notes = []        # Collected (timestamp, note_value) for the target difficulty
    
    # Process global metadata (before any COURSE definition)
    for line in lines:
        line_stripped = line.strip()
        if not line_stripped or line_stripped.startswith("//"):
            continue
        # Stop reading global metadata once a COURSE line is encountered
        if line_stripped.upper().startswith("COURSE:"):
            break
        if line_stripped.upper().startswith("WAVE:"):
            mp3_path = line_stripped.split(":", 1)[1].strip()
        elif line_stripped.upper().startswith("GENRE:"):
            genre = line_stripped.split(":", 1)[1].strip()
        elif line_stripped.upper().startswith("BPM:"):
            try:
                base_bpm = float(line_stripped.split(":", 1)[1].strip())
            except ValueError:
                base_bpm = 120.0
        elif line_stripped.upper().startswith("OFFSET:"):
            try:
                off = float(line_stripped.split(":", 1)[1].strip())
                # If offset is large (e.g., 1000), treat it as milliseconds
                if abs(off) > 50:
                    offset_sec = off / 1000.0
                else:
                    offset_sec = off
            except ValueError:
                offset_sec = 0.0

    current_bpm = base_bpm  # Set initial BPM

    # --- Process Chart (Target Difficulty) ---
    for line in lines:
        line_stripped = line.strip()
        if not line_stripped or line_stripped.startswith("//"):
            continue

        if not in_chart:
            # Look for a COURSE definition to match the target difficulty
            if line_stripped.upper().startswith("COURSE:"):
                course_value = line_stripped.split(":", 1)[1].strip()
                parsing_target = (course_value.lower() == target_difficulty.lower())
                if parsing_target:
                    difficulty_str = course_value  # Use exact value from file
            if line_stripped.upper().startswith("#START"):
                # Begin chart section
                if parsing_target:
                    in_chart = True
                    # Reset timing and measure info for this chart section
                    current_time = 0.0
                    current_bpm = base_bpm
                    measure_numer = 4.0
                    measure_denom = 4.0
                    measure_start_bpm = current_bpm
                    notes_buffer = []
                    bpm_events = []
                    output_notes = []  # Start fresh for the target difficulty notes
                    # Apply offset adjustment
                    current_time -= offset_sec * 1000.0
                else:
                    in_chart = True  # Still enter to skip non-target difficulties
                continue
        else:
            # Inside a #START ... #END block
            if line_stripped.upper().startswith("#END"):
                if parsing_target:
                    # Finished processing the target chart
                    break
                else:
                    in_chart = False
                    continue
            
            # If not in the target difficulty, skip the contents
            if not parsing_target:
                continue
            
            # Process commands that affect timing
            if line_stripped.upper().startswith("#BPMCHANGE"):
                parts = line_stripped.split()
                if len(parts) > 1:
                    try:
                        new_bpm = float(parts[1])
                    except ValueError:
                        continue
                    if len(notes_buffer) == 0:
                        # BPM change at the very start of the measure
                        measure_start_bpm = new_bpm
                    else:
                        # BPM change after some notes in the current measure
                        bpm_events.append((len(notes_buffer), new_bpm))
                    current_bpm = new_bpm
                continue
            
            if line_stripped.upper().startswith("#MEASURE"):
                data = ""
                if " " in line_stripped:
                    data = line_stripped.split(None, 1)[1]
                elif ":" in line_stripped:
                    data = line_stripped.split(":", 1)[1]
                data = data.strip()
                if "/" in data:
                    num_str, den_str = data.split("/", 1)
                    try:
                        num = float(num_str)
                        den = float(den_str)
                        if den != 0:
                            measure_numer = num
                            measure_denom = den
                    except ValueError:
                        pass
                continue
            
            if line_stripped.upper().startswith("#DELAY"):
                parts = line_stripped.split()
                if len(parts) > 1:
                    try:
                        delay_seconds = float(parts[1])
                    except ValueError:
                        continue
                    current_time += delay_seconds * 1000.0
                continue
            
            if (line_stripped.upper().startswith("#SCROLL") or 
                line_stripped.upper().startswith("#GOGOSTART") or 
                line_stripped.upper().startswith("#GOGOEND") or 
                line_stripped.upper().startswith("#BRANCH")):
                continue

            # Process note lines: read digit characters until a comma (measure end)
            for ch in line_stripped:
                if ch.isdigit():
                    notes_buffer.append(int(ch))
                elif ch == ',':
                    total_notes = len(notes_buffer)
                    if total_notes > 0:
                        # Calculate the measure's duration in quarter-beats
                        measure_quarter_beats = (measure_numer / measure_denom) * 4.0
                        local_bpm = measure_start_bpm
                        idx = 0
                        bpm_events.sort(key=lambda x: x[0])
                        for evt_idx, evt_bpm in bpm_events:
                            # Process notes up to the BPM change event
                            while idx < evt_idx and idx < total_notes:
                                note = notes_buffer[idx]
                                # Only process TJA note types 1-4
                                if note in (1, 2, 3, 4):
                                    # Map the TJA note to .bin note:
                                    # TJA 1 -> 0 (Little Don), TJA 2 -> 1 (Little Ka),
                                    # TJA 3 -> 2 (Big Don), TJA 4 -> 3 (Big Ka)
                                    note_val = note - 1
                                    output_notes.append((int(round(current_time)), note_val))
                                # Advance time by one subdivision
                                time_per_subdiv = (60.0 / local_bpm) * 1000.0 * (measure_quarter_beats / total_notes)
                                current_time += time_per_subdiv
                                idx += 1
                            if idx == evt_idx:
                                local_bpm = evt_bpm  # Switch BPM at the event
                        # Process any remaining notes after the last BPM change
                        while idx < total_notes:
                            note = notes_buffer[idx]
                            if note in (1, 2, 3, 4):
                                note_val = note - 1
                                output_notes.append((int(round(current_time)), note_val))
                            time_per_subdiv = (60.0 / local_bpm) * 1000.0 * (measure_quarter_beats / total_notes)
                            current_time += time_per_subdiv
                            idx += 1
                    else:
                        # If the measure is empty, advance by the full measure duration
                        measure_duration_ms = (60.0 / current_bpm) * 1000.0 * ((measure_numer / measure_denom) * 4.0)
                        current_time += measure_duration_ms
                    # Clear measure buffers and update starting BPM for next measure
                    notes_buffer.clear()
                    bpm_events.clear()
                    measure_start_bpm = current_bpm

    # --- Write the .bin file ---
    with open(bin_path, 'w', encoding='utf-8') as fout:
        fout.write(f"PATH_TO_MP3_FILE: {mp3_path}\n")
        fout.write(f"GENRE: {genre}\n")
        fout.write(f"DIFFICULTY: {difficulty_str}\n")
        fout.write(f"BPM: {base_bpm}\n")
        fout.write("\n")
        fout.write("Note Timing:\n")
        for timestamp, note_val in output_notes:
            fout.write(f"{format_timestamp(timestamp)} {note_val}\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Convert a .tja chart file to .bin format for the Taiko simulator using only four note types."
    )
    parser.add_argument(
        "tja_path", 
        help="Path to the input .tja file"
    )
    parser.add_argument(
        "bin_path", 
        help="Path to the output .bin file"
    )
    parser.add_argument(
        "--difficulty",
        default="Normal",
        help="Target difficulty to parse (default: Normal). E.g., use '9' to match DIFFICULTY: 9 in the output."
    )
    args = parser.parse_args()
    
    convert_tja_to_bin(args.tja_path, args.bin_path, args.difficulty)
