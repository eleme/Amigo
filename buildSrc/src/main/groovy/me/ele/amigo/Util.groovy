package me.ele.amigo

import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class Util {


    static String findFileInDir(String fileName, String path) {
        List<String> list = findAllFiles(path)
        for (String name : list) {
            if (name.endsWith(fileName)) {
                return name
            }
        }

        return null
    }

    static List<String> findAllFiles(String path) {
        List<String> list = new ArrayList<>()
        File file = new File(path)
        if (file.isFile()) {
            list.add(file.absolutePath)
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                list.addAll(findAllFiles(f.absolutePath))
            }
        }

        return list
    }


    static void addFilesToExistingZip(File zipFile, File[] files, entryName) throws IOException {
        File tempFile = File.createTempFile(zipFile.getName(), null);
        // delete it, otherwise you cannot rename your existing zip to it.
        tempFile.delete();

        boolean renameOk = zipFile.renameTo(tempFile);
        if (!renameOk) {
            throw new RuntimeException("could not rename the file " + zipFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }
        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            boolean notInFiles = true;
            for (File f : files) {
                if (f.getName().equals(name)) {
                    notInFiles = false;
                    break;
                }
            }
            if (notInFiles) {
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        // Close the streams
        zin.close();
        // Compress the files
        for (int i = 0; i < files.length; i++) {
            InputStream inputStream = new FileInputStream(files[i]);
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(entryName));
            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            inputStream.close();
        }
        // Complete the ZIP file
        out.close();
        tempFile.delete();
    }

    public static void deleteZipEntry(File zipFile, List<String> files) throws IOException {
        // get a temp file
        File tempFile = File.createTempFile(zipFile.getName(), null);
        // delete it, otherwise you cannot rename your existing zip to it.
        tempFile.delete();
        tempFile.deleteOnExit();
        boolean renameOk = zipFile.renameTo(tempFile);
        if (!renameOk) {
            throw new RuntimeException("could not rename the file " + zipFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }
        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            boolean toBeDeleted = false;
            for (String f : files) {
                if (f.equals(name)) {
                    toBeDeleted = true;
                    break;
                }
            }
            if (!toBeDeleted) {
                // Add ZIP entry to output stream.
                zout.putNextEntry(new ZipEntry(name));
                // Transfer bytes from the ZIP file to the output file
                int len;
                while ((len = zin.read(buf)) > 0) {
                    zout.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        zin.close();
        zout.close();
        tempFile.delete();
    }

    public static boolean containsProject(Project project, String projectName) {
        Set<Project> projects = project.rootProject.subprojects;
        boolean ret = false
        projects.each { Project p ->
            if (p.name == projectName) {
                ret = true
            }
        }
        return ret
    }
}