package de.oglimmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.google.common.io.CharStreams;

public class Main {

	static FileWriter log;
	static int counter = 0;
	static long time = System.currentTimeMillis();

	public static void main(String[] args) throws IOException, DocumentException, InterruptedException {

		log = new FileWriter("log");

		SAXReader reader = new SAXReader();
		Document document = reader.read(new File("source.xml"));

		Element root = document.getRootElement();
		for (Iterator<Element> i = root.elementIterator("dict"); i.hasNext();) {
			Element dictLevel1 = i.next();
			for (Iterator<Element> j = dictLevel1.elementIterator("dict"); j.hasNext();) {
				Element dictLevel2 = j.next();
				for (Iterator<Element> k = dictLevel2.elementIterator("dict"); k.hasNext();) {
					Element dict = k.next();
					processActualTrackDict(dict);
				}
			}

		}

		log.close();

	}

	private static void processActualTrackDict(Element dict)
			throws FileNotFoundException, IOException, InterruptedException {
		Track track = new Track(dict);
		track.process();
		handleCounter();
	}

	private static void handleCounter() {
		counter++;
		if (counter % 10 == 0) {
			System.out.println("~~~~~~~~~~~~~~~~~~~~" + (System.currentTimeMillis() - time) + "/" + counter);
			time = System.currentTimeMillis();
		}
	}

	static class Track {

		String name;
		String artist;
		String album;
		int intRating;
		int intPlayCount;

		public Track(Element dict) {
			name = get(dict, "Name");
			artist = get(dict, "Artist");
			album = get(dict, "Album");
			String rating = get(dict, "Rating");
			intRating = 0;
			try {
				intRating = Integer.parseInt(rating);
			} catch (NumberFormatException e) {
			}
			String playCount = get(dict, "Play Count");
			intPlayCount = 0;
			try {
				intPlayCount = Integer.parseInt(playCount);
			} catch (NumberFormatException e) {
			}
		}

		public void process() throws FileNotFoundException, IOException, InterruptedException {
			if (intPlayCount >= 1 || intRating >= 10) {
				System.out.println(String.format("%s, %s, %s, %s, %s", name, artist, album, intRating, intPlayCount));
				callAppleScript(log, name, artist, album, intRating, intPlayCount);
			}
		}
	}

	private static void callAppleScript(FileWriter log, String name, String artist, String album, int rating,
			int playedCount) throws FileNotFoundException, IOException, InterruptedException {
		writeDataFile(name, artist, album, rating, playedCount);

		Process p = new ProcessBuilder("osascript", "a.scpt").start();
		String text = CharStreams.toString(new InputStreamReader(p.getErrorStream()));
		String out = CharStreams.toString(new InputStreamReader(p.getInputStream()));

		p.waitFor();

		if (out.startsWith("-1")) {
			log.write("ERROR\r\n");
			log.write(text + "\r\n");
			log.flush();
		} else if (!out.startsWith("1")) {
			log.write("MULTI_HIT\r\n");
			log.write(text + "\r\n");
			log.flush();
		}
	}

	private static void writeDataFile(String name, String artist, String album, int rating, int playedCount)
			throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream("data");
		fos.write((artist + "	" + album + "	" + name + "	" + rating + "	" + playedCount).getBytes());
		fos.close();
	}

	private static String get(Element root, String key) {
		for (Iterator<Element> i = root.elementIterator(); i.hasNext();) {
			Element ele = i.next();
			if (ele.getData().toString().equals(key)) {
				Element valEle = (Element) i.next();
				return valEle.getData().toString();
			}
		}
		return "";
	}

}
