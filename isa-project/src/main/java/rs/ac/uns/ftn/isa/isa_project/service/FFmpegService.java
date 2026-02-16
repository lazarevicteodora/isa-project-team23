package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servis za transcoding video fajlova korišćenjem FFmpeg-a.
 *
 * FFmpeg mora biti instaliran i dostupan u PATH-u.
 * Otići na https://www.gyan.dev/ffmpeg/builds/
 *i skinuti ffmpeg-release-full.7z
 */
@Service
public class FFmpegService {

    private static final Logger LOG = LoggerFactory.getLogger(FFmpegService.class);

    // Mapiranje rezolucija na visinu videa (širina se automatski kalkuliše)
    private static final Map<String, String> RESOLUTION_MAP = Map.of(
            "1080p", "1920:1080",
            "720p", "1280:720",
            "480p", "854:480",
            "360p", "640:360"
    );

    /**
     * Proverava da li je FFmpeg instaliran na sistemu.
     */
    public boolean isFFmpegInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            LOG.error("FFmpeg is not installed or not in PATH: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Transcode-uje video u specifičnu rezoluciju.
     *
     * @param inputPath Putanja do originalnog videa
     * @param outputPath Putanja gde će se sačuvati transcode-ovani video
     * @param resolution Target rezolucija (npr. "720p", "480p")
     * @return true ako je transcoding uspešan, false inače
     */
    public boolean transcodeVideo(String inputPath, String outputPath, String resolution) {
        LOG.info("Starting transcoding: {} -> {} ({})", inputPath, outputPath, resolution);

        if (!isFFmpegInstalled()) {
            LOG.error("FFmpeg is not installed!");
            return false;
        }

        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            LOG.error("Input file does not exist: {}", inputPath);
            return false;
        }

        try {
            // Kreiraj output direktorijum ako ne postoji
            Path outputDir = Paths.get(outputPath).getParent();
            if (outputDir != null && !Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Pripremi FFmpeg komandu
            List<String> command = buildFFmpegCommand(inputPath, outputPath, resolution);

            LOG.debug("FFmpeg command: {}", String.join(" ", command));

            // Pokreni FFmpeg proces
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Čitaj output (za progress tracking)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.trace("FFmpeg: {}", line);
                    // Možeš parsirati progress ako želiš
                    parseProgress(line);
                }
            }

            // Čekaj da se proces završi
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                LOG.info("Transcoding completed successfully: {}", outputPath);
                return true;
            } else {
                LOG.error("Transcoding failed with exit code: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            LOG.error("Error during transcoding: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Transcode-uje video u više rezolucija odjednom.
     *
     * @param inputPath Putanja do originalnog videa
     * @param outputDirectory Direktorijum gde će se čuvati output fajlovi
     * @param resolutions Lista target rezolucija
     * @return Mapa (rezolucija -> output path) za uspešne transcode-ove
     */
    public Map<String, String> transcodeMultipleResolutions(
            String inputPath,
            String outputDirectory,
            List<String> resolutions) {

        LOG.info("Starting multi-resolution transcoding: {} resolutions", resolutions.size());

        Map<String, String> results = new HashMap<>();
        String baseName = getFileNameWithoutExtension(inputPath);

        for (String resolution : resolutions) {
            String outputPath = outputDirectory + File.separator +
                    baseName + "_" + resolution + ".mp4";

            boolean success = transcodeVideo(inputPath, outputPath, resolution);

            if (success) {
                results.put(resolution, outputPath);
            } else {
                LOG.warn("Failed to transcode to resolution: {}", resolution);
            }
        }

        LOG.info("Transcoding completed: {}/{} successful",
                results.size(), resolutions.size());

        return results;
    }

    /**
     * Gradi FFmpeg komandu za transcoding.
     */
    private List<String> buildFFmpegCommand(String inputPath, String outputPath, String resolution) {
        List<String> command = new ArrayList<>();

        command.add("ffmpeg");
        command.add("-i");
        command.add(inputPath);

        // Video codec: H.264 (najkompatibilniji)
        command.add("-c:v");
        command.add("libx264");

        // Audio codec: AAC
        command.add("-c:a");
        command.add("aac");

        // Rezolucija
        String scale = RESOLUTION_MAP.getOrDefault(resolution, "1280:720");
        command.add("-vf");
        command.add("scale=" + scale);

        // Bitrate (prilagodi prema rezoluciji)
        String videoBitrate = getVideoBitrate(resolution);
        command.add("-b:v");
        command.add(videoBitrate);

        // Audio bitrate
        command.add("-b:a");
        command.add("128k");

        // Preset (balance između brzine i kvaliteta)
        command.add("-preset");
        command.add("medium");

        // Overwrite output file without asking
        command.add("-y");

        // Output file
        command.add(outputPath);

        return command;
    }

    /**
     * Određuje video bitrate na osnovu rezolucije.
     */
    private String getVideoBitrate(String resolution) {
        return switch (resolution) {
            case "1080p" -> "5000k";
            case "720p" -> "2500k";
            case "480p" -> "1000k";
            case "360p" -> "500k";
            default -> "2500k";
        };
    }

    /**
     * Parsira progress iz FFmpeg output-a (opciono, za naprednije tracking).
     */
    private void parseProgress(String line) {
        // FFmpeg ispisuje progress u formatu:
        // frame= 1234 fps=30 q=28.0 size=    5120kB time=00:00:41.40 bitrate=1013.2kbits/s speed=1.2x

        Pattern pattern = Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2})");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            int hours = Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            int seconds = Integer.parseInt(matcher.group(3));
            int totalSeconds = hours * 3600 + minutes * 60 + seconds;

            LOG.trace("Transcoding progress: {}s", totalSeconds);
            // Ovde možeš update-ovati progress u bazi
        }
    }

    /**
     * Dobija ime fajla bez ekstenzije.
     */
    private String getFileNameWithoutExtension(String filePath) {
        File file = new File(filePath);
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? name : name.substring(0, dotIndex);
    }

    /**
     * Dobija informacije o videu (trajanje, rezolucija, itd.)
     */
    public Map<String, String> getVideoInfo(String videoPath) {
        Map<String, String> info = new HashMap<>();

        try {
            List<String> command = Arrays.asList(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-show_entries", "stream=width,height",
                    "-of", "default=noprint_wrappers=1",
                    videoPath
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        info.put(parts[0], parts[1]);
                    }
                }
            }

            process.waitFor();

        } catch (Exception e) {
            LOG.error("Error getting video info: {}", e.getMessage());
        }

        return info;
    }
}