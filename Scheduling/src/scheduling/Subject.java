package scheduling;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
/**
 *
 * @author leijurv
 */
public class Subject {
    final String name;
    final ArrayList<Klass> klasses;
    public final ArrayList<Teacher> teachers;
    public Subject(String name, ArrayList<Klass> klasses, Teacher[] teachers) {
        this.teachers = new ArrayList<>(Arrays.asList(teachers));
        this.klasses = klasses;
        this.name = name;
        registerKlasses();
    }
    public Subject(String name, String[] klassNames, int[] sectionNumbers, Teacher[] teachers) {
        this.teachers = new ArrayList<>(Arrays.asList(teachers));
        if (klassNames.length != sectionNumbers.length) {
            throw new IllegalArgumentException("Different numbers of klassNames and sectionNumbers");
        }
        this.name = name;
        int numKlasses = klassNames.length;
        klasses = new ArrayList<>(numKlasses);
        Random r = new Random();
        int nr = Room.numRooms;
        for (int i = 0; i < numKlasses; i++) {
            Klass klass = new Klass(klassNames[i], sectionNumbers[i]);
            klass.resetAcceptableRooms();
            for (int j = 0; j < 5; j++) {
                klass.addAcceptableRoom(Room.getRoomArray()[r.nextInt(nr)]);
            }
            klasses.add(klass);
            klass.registerSubject(this);
            System.out.println("Klass " + klass + " has rooms " + klass.acceptableRooms);
        }
        this.teachers.parallelStream().forEach((t)->{
            t.subjectsTeached.add(this);
        });
    }
    public final void registerKlasses() {
        klasses.stream().parallel().forEach((klass)->{//This is thread safe because ArrayList.contains is thread safe
            klass.registerSubject(this);
        });
    }
    public boolean hasKlass(Klass klass) {
        return klasses.contains(klass);
    }
    @Override
    public String toString() {
        return name;
    }
}
