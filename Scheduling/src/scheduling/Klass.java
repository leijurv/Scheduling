package scheduling;
import java.util.ArrayList;
import java.util.Arrays;
/**
 *
 * @author leijurv
 */
public class Klass {
    private Subject subject = null;
    final String name;
    final int numSections;
    final Section[] sections;
    final ArrayList<Teacher> teachers;
    ArrayList<Room> acceptableRooms;
    public Klass(String name, int numSections, Teacher[] customTeachers, ArrayList<Room> acceptableRooms) {
        this.name = name;
        this.numSections = numSections;
        sections = new Section[numSections];
        for (int i = 0; i < numSections; i++) {
            sections[i] = new Section(this, i);
        }
        teachers = new ArrayList<>(Arrays.asList(customTeachers));
        this.acceptableRooms = new ArrayList<>(acceptableRooms);
    }
    public Klass(String name, int numSections, Teacher[] customTeachers) {
        this(name, numSections, customTeachers, Room.getRooms());
    }
    public Klass(String name, int numSections) {
        this(name, numSections, new Teacher[] {});
    }
    public void registerSubject(Subject s) {
        if (subject != null) {
            throw new IllegalStateException("Already has subject");
        }
        if (!s.hasKlass(this)) {
            throw new IllegalStateException("YOU ARE NOT MY REAL SUBJECT");
        }
        teachers.addAll(s.teachers);
        subject = s;
    }
    public Subject getSubject() {
        return subject;
    }
    public void setAcceptableRoom(Room room) {
        resetAcceptableRooms();
        addAcceptableRoom(room);
    }
    public void addAcceptableRoom(Room room) {
        acceptableRooms.add(room);
    }
    public void resetAcceptableRooms() {
        acceptableRooms = new ArrayList<>();
    }
    @Override
    public String toString() {
        return subject + "--" + name;
    }
    public ArrayList<Teacher> getTeachers() {
        return teachers;
    }
}
