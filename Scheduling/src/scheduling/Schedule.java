package scheduling;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 *
 * @author leijurv
 */
public class Schedule {
    public final HashMap<Section, Room> locations;
    public final HashMap<Section, Block> timings;
    public final HashMap<Section, Teacher> teachers;
    public final ArrayList<Section> sections;
    public final ArrayList<Student> students;
    public final Roster roster;
    public ArrayList<Teacher> teacherList;
    public Schedule(ArrayList<Section> sections, ArrayList<Student> students) {
        locations = new HashMap<>();
        timings = new HashMap<>();
        teachers = new HashMap<>();
        this.sections = sections;
        this.students = students;
        this.roster = new Roster(students, sections);
        //rebuildTeacherList();
        this.teacherList = Scheduling.tempTeacherList;
    }
    public boolean verifyRoomsTeachers() {
        for (Block block : Block.blocks) {
            if (!Room.getRooms().parallelStream().noneMatch((room)->(getRoomUsage(room, block).size() > 1))) {
                return false;
            }
            if (!teacherList.parallelStream().noneMatch((t)->(getTeacherLocation(t, block).size() > 1))) {
                return false;
            }
        }
        return true;
    }
    public static class Tuple<A, B> {
        public A a;
        public B b;
        public Tuple(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }
    public String findConflicts() {
        Stream<Tuple<Room, List<String>>> roomConf
                = Room.getRooms().parallelStream().map(room->new Tuple<Room, List<String>>(room,
                                Stream.of(Block.blocks).parallel().map(
                                        block->new Tuple<Block, List<Section>>(
                                                block,
                                                sections.parallelStream().filter(section->room.equals(locations.get(section))
                                                        && block.equals(timings.get(section))).collect(Collectors.toList())
                                        )
                                ).filter(tuple->tuple.b.size() > 1).
                                map(tuple->"\"In room " + room + " during " + tuple.a + ", there are classes " + tuple.b + "\"").
                                collect(Collectors.toList()))
                ).filter(tuple->!tuple.b.isEmpty());
        Stream<Tuple<Teacher, List<String>>> teacherConf = teacherList.parallelStream().map(teacher->new Tuple<Teacher, List<String>>(teacher,
                Stream.of(Block.blocks).parallel().map(
                        block->new Tuple<Block, List<Section>>(
                                block,
                                sections.parallelStream().filter(section->teacher.equals(teachers.get(section)) && block.equals(timings.get(section))).collect(Collectors.toList())
                        )
                ).filter(tuple->tuple.b.size() > 1).map(tuple->"\"Teacher " + teacher + " during " + tuple.a + " is teaching classes " + tuple.b + "\"").collect(Collectors.toList()))
        ).filter(tuple->!tuple.b.isEmpty());
        String roomConflicts = roomConf.map(tuple->"\n\"" + tuple.a + "\":" + tuple.b).collect(Collectors.joining(",", "{", "}"));
        String teacherConflicts = teacherConf.map(tuple->"\n\"" + tuple.a + "\":" + tuple.b).collect(Collectors.joining(",", "{", "}"));
        Stream<Tuple<Student, List<String>>> studentConf = students.stream().map(student->{
            Map<Block, List<Section>> map = roster.getSections(student).stream().collect(Collectors.groupingBy(section->timings.get(section)));
            List<String> conf = Stream.of(Block.blocks).map(block->new Tuple<Block, List<Section>>(block, map.get(block))).filter(tuple->tuple.b != null && tuple.b.size() > 1).map(tuple->"\"Student " + student + " during " + tuple.a + " is in classes " + tuple.b + "\"").collect(Collectors.toList());
            return new Tuple<Student, List<String>>(student, conf);
        }).filter(tuple->!tuple.b.isEmpty());
        String studentConflicts = studentConf.map(tuple->"\n\"" + tuple.a + "\":" + tuple.b).collect(Collectors.joining(",", "{", "}"));
        //System.out.println(roomConflicts);
        //System.out.println(teacherConflicts);
        return "{room:" + roomConflicts + ",\nteacher:" + teacherConflicts + ",\nstudent:" + studentConflicts + "}";
        /*
         Map<Room, Map<Block, List<Section>>> loc1
         = Room.getRooms().parallelStream().collect(Collectors.toMap(room->room,
         room->Stream.of(Block.blocks).parallel().collect(Collectors.toMap(
         block->block,
         block->sections.parallelStream().
         filter(section->room.equals(locations.get(section)) && block.equals(timings.get(section))).collect(Collectors.toList())
         ))));*/
    }
    public List<Section> getTeacherLocation(Teacher teacher, Block time) {//well, the teacher SHOULD only be in one place...
        //this can be parallel because hashmaps are thread safe if the only operations happening are read only
        return sections.parallelStream().filter(section->time.equals(timings.get(section)) && teacher.equals(teachers.get(section))).collect(Collectors.toList());
    }
    public List<Section> getRoomUsage(Room room, Block time) {//well, there SHOULD only be one section in each room at once...
        return sections.parallelStream().filter(section->time.equals(timings.get(section)) && room.equals(locations.get(section))).collect(Collectors.toList());
    }
    public boolean isTeacherUnoccupied(Teacher teacher, Block time) {
        return sections.parallelStream().noneMatch(section->time.equals(timings.get(section)) && teacher.equals(teachers.get(section)));
    }
    public boolean isRoomEmpty(Room room, Block time) {
        return sections.parallelStream().noneMatch(section->time.equals(timings.get(section)) && room.equals(locations.get(section)));
    }
    public Stream<Section> getStudentSectionStream(Student student, Block time) {//well, the student SHOULD only be in one section at once..
        return roster.getSections(student).stream().filter(section->time.equals(timings.get(section)));
    }
    public boolean studentIsInClass(Student student, Block time) {
        return roster.getSections(student).stream().anyMatch(section->time.equals(timings.get(section)));
    }
    public List<Section> getStudentSection(Student student, Block time) {
        return getStudentSectionStream(student, time).collect(Collectors.toList());
    }
    public List<Room> getStudentLocation(Student student, Block time) {//well, the student SHOULD only be in one room at once...
        return getStudentSectionStream(student, time).map(section->locations.get(section)).collect(Collectors.toList());
    }
    public Map<Block, Section> getScheduleOrig(Predicate<Section> pred) {//this version throws an illegalstateexception if there are conflicts
        return sections.parallelStream()
                .filter(pred)//get the sections that this teacher is teaching
                .collect(Collectors.toMap(section->timings.get(section), section->section));//map of timings.get(section) to section
    }
    public Map<Block, Section> getSingleSchedule(Predicate<Section> pred) {//this version is slower (I think), but it doesn't matter
        List<Section> k = sections.parallelStream()
                .filter(pred).collect(Collectors.toList());
        return Stream.of(Block.blocks).parallel().map(block
                ->new Tuple<Block, Section>(block,
                        get(
                                k.parallelStream()
                                .filter(section->block.equals(timings.get(section))).findAny()))).filter(tuple->tuple.b != null).collect(Collectors.toMap(tuple->tuple.a, tuple->tuple.b));
    }
    public <A> A get(Optional<A> k) {
        if (k.isPresent()) {
            return k.get();
        }
        return null;
    }
    public Map<Block, List<Section>> getSchedule(Predicate<Section> pred) {//this version is slower (I think), but it doesn't matter
        return convert(sections.parallelStream().filter(pred));
    }
    public Map<Block, List<Section>> convert(Stream<Section> k) {
        return k.collect(Collectors.groupingBy(section->timings.get(section)));
    }
    public Map<Block, List<Section>> convert1(List<Section> k) {
        return Stream.of(Block.blocks).parallel().map(block
                ->new Tuple<Block, List<Section>>(block,
                        k.parallelStream()
                        .filter(section->block.equals(timings.get(section))).collect(Collectors.toList())
                )).filter(tuple->tuple.b != null).collect(Collectors.toMap(tuple->tuple.a, tuple->tuple.b));
    }
    public Map<Block, List<Section>> getTeacherSchedule(Teacher teacher) {
        return getSchedule(section->teacher.equals(teachers.get(section)));
    }
    public Map<Block, List<Section>> getRoomSchedule(Room room) {
        return getSchedule(section->room.equals(locations.get(section)));
    }
    public Map<Block, List<Section>> getStudentSchedule(Student student) {
        //return roster.getSections(student).parallelStream().collect(Collectors.toMap(section->timings.get(section), section->section));
        return convert(roster.getSections(student).stream());
    }
    public Map<Teacher, Map<Block, List<Section>>> getTeacherSchedules() {
        return teacherList.parallelStream().collect(Collectors.toMap(teacher->teacher, teacher->getTeacherSchedule(teacher)));
    }
    public Map<Student, Map<Block, List<Section>>> getStudentSchedules() {
        return students.parallelStream().collect(Collectors.toMap(student->student, student->getStudentSchedule(student)));
    }
    public Map<Room, Map<Block, List<Section>>> getRoomSchedules() {
        return Room.getRooms().parallelStream().collect(Collectors.toMap(room->room, room->getRoomSchedule(room)));
    }
    public static void output(Scheduler rd) throws IOException {
        Schedule schedule = rd.getResult();
        Map<Teacher, Map<Block, List<Section>>> teacherSchedules = schedule.getTeacherSchedules();
        Map<Room, Map<Block, List<Section>>> roomSchedules = schedule.getRoomSchedules();
        Map<Student, Map<Block, List<Section>>> studentSchedules = schedule.getStudentSchedules();
        String basePath = System.getProperty("user.home") + "/Documents/schedout/";
        File base = new File(basePath);
        File main = new File(basePath + "sections.csv");
        schedule.roster.write(new File(basePath + "roster"));
        try(FileWriter writer = new FileWriter(main)) {
            writer.write("Section,Block,Teacher,Room\n");
            for (Section section : schedule.sections) {
                writer.write(section + "," + schedule.timings.get(section).blockID + "," + schedule.teachers.get(section) + "," + schedule.locations.get(section).roomNumber + "\n");
            }
        }
        new File(basePath + "teachers").mkdir();
        new File(basePath + "students").mkdir();
        new File(basePath + "rooms").mkdir();
        for (Teacher teacher : rd.teachers) {
            write(base, teacherSchedules.get(teacher), schedule, "teachers", teacher.toString());
        }
        for (Student student : rd.students) {
            write(base, studentSchedules.get(student), schedule, "students", student.toString());
        }
        for (Room room : Room.getRoomArray()) {
            write(base, roomSchedules.get(room), schedule, "rooms", room.toString());
        }
    }
    public static void write(File main, Map<Block, List<Section>> data, Schedule schedule, String folderName, String thisName) throws IOException {
        main = new File(main.toString() + File.separatorChar + folderName + File.separatorChar + thisName + ".csv");
        try(FileWriter writer = new FileWriter(main)) {
            writer.write("Section,Block,Teacher,Room\n");
            for (Block b : data.keySet()) {
                for (Section section : data.get(b)) {
                    writer.write(section + "," + schedule.timings.get(section).blockID + "," + schedule.teachers.get(section) + "," + schedule.locations.get(section).roomNumber + "\n");
                }
            }
        }
    }
    public final void rebuildTeacherList() {
        teacherList = getTeacherList(sections);
    }
    public static ArrayList<Teacher> getTeacherList(ArrayList<Section> sections) {
        return sections.stream().flatMap(section->section.klass.teachers.stream()).distinct().collect(Collectors.toCollection(()->new ArrayList<>()));
    }
}
