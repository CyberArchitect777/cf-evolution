/*
 * CF Evolution: An editor for Formula One Grand Prix/World Circuit
 * Copyright (C) 2005-2007  The Chequered Flag Development Team
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package cfevolution.data.dataset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A game dataset: everything CF Evolution can edit, by meaning rather than
 * by offset, so it moves between installs and builds. The text is an ini
 * file (.cfe): [sections] of "key = value" lines, ";" starting a comment.
 * Pictures (PNG) and circuits travel as further files in folders; a dataset
 * with any such file is written as a zip holding the .cfe and the folders,
 * and one without as the .cfe alone (owner's decision, 2026-10-25).
 *
 * @author barrie
 */
public class Dataset
{
    public static final String EXTENSION = ".cfe";
    public static final int FORMAT = 1;

    private final Map<String, Map<String, String>> sections = new LinkedHashMap<String, Map<String, String>>();
    private final Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
    /** Things a reader of another editor's file had to leave out, for the import report. */
    private final List<String> notes = new ArrayList<String>();

    public Dataset()
    {
    }

    // --- contents ---------------------------------------------------------

    public boolean hasSection(String name)
    {
        return sections.containsKey(name);
    }

    public Map<String, String> section(String name)
    {
        Map<String, String> s = sections.get(name);
        if (s == null)
        {
            s = new LinkedHashMap<String, String>();
            sections.put(name, s);
        }
        return s;
    }

    public List<String> sectionNames()
    {
        return new ArrayList<String>(sections.keySet());
    }

    public String get(String section, String key)
    {
        Map<String, String> s = sections.get(section);
        return s == null ? null : s.get(key);
    }

    public void put(String section, String key, String value)
    {
        section(section).put(key, value);
    }

    public void put(String section, String key, int value)
    {
        put(section, key, String.valueOf(value));
    }

    /** A further file, by its path in the dataset (e.g. "helmets/car07.png"). */
    public void putFile(String path, byte[] data)
    {
        files.put(path, data);
    }

    public byte[] getFile(String path)
    {
        return files.get(path);
    }

    public List<String> filePaths()
    {
        return new ArrayList<String>(files.keySet());
    }

    public void addNote(String note)
    {
        if (!notes.contains(note))
            notes.add(note);
    }

    public List<String> getNotes()
    {
        return notes;
    }

    // --- the text ---------------------------------------------------------

    public static Dataset parse(String text)
    {
        Dataset d = new Dataset();
        String current = null;
        for (String raw : text.split("\r\n|\n|\r"))
        {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#"))
                continue;
            if (line.startsWith("[") && line.endsWith("]"))
            {
                current = line.substring(1, line.length() - 1).trim().toLowerCase();
                d.section(current);
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0 || current == null)
                continue;
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
                value = value.substring(1, value.length() - 1);     // quoted to keep spaces at either end
            d.put(current, line.substring(0, eq).trim().toLowerCase(), value);
        }
        return d;
    }

    public String format()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("; CF Evolution game dataset. Plain text: each [section] holds \"name = value\" lines.\n");
        sb.append("; Colours are numbers in the game's palette (0-255). Anything left out is not changed on import.\n");
        sb.append("; A value in \"quotes\" keeps the spaces at its ends.\n");
        for (Map.Entry<String, Map<String, String>> s : sections.entrySet())
        {
            sb.append('\n').append('[').append(s.getKey()).append("]\n");
            for (Map.Entry<String, String> e : s.getValue().entrySet())
            {
                String v = e.getValue();
                if (!v.equals(v.trim()) || v.startsWith("\""))
                    v = "\"" + v + "\"";
                sb.append(e.getKey()).append(" = ").append(v).append('\n');
            }
        }
        return sb.toString();
    }

    // --- files --------------------------------------------------------------

    /** True if this dataset needs a zip: it carries more than the text. */
    public boolean needsZip()
    {
        return !files.isEmpty();
    }

    /**
     * Writes the dataset: a .cfe alone, or a zip holding name.cfe and the
     * folders. Returns the file written (its extension set to suit).
     */
    public File write(File target) throws IOException
    {
        String name = target.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        byte[] text = format().replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
        if (!needsZip())
        {
            File out = new File(target.getParentFile(), base + EXTENSION);
            Files.write(out.toPath(), text);
            return out;
        }
        File out = new File(target.getParentFile(), base + ".zip");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(new ZipEntry(base + EXTENSION));
        zip.write(text);
        zip.closeEntry();
        for (Map.Entry<String, byte[]> f : files.entrySet())
        {
            zip.putNextEntry(new ZipEntry(f.getKey()));
            zip.write(f.getValue());
            zip.closeEntry();
        }
        zip.close();
        Files.write(out.toPath(), bytes.toByteArray());
        return out;
    }

    /** Reads a .cfe, or a zip holding one (other files in the zip become the dataset's files). */
    public static Dataset read(File file) throws IOException
    {
        byte[] d = Files.readAllBytes(file.toPath());
        if (!isZip(d))
            return parse(text(d));
        Map<String, byte[]> entries = unzip(d);
        Dataset dataset = null;
        String folder = "";
        for (Map.Entry<String, byte[]> e : entries.entrySet())
            if (e.getKey().toLowerCase().endsWith(EXTENSION))
            {
                dataset = parse(text(e.getValue()));
                int slash = e.getKey().lastIndexOf('/');
                folder = slash < 0 ? "" : e.getKey().substring(0, slash + 1);
                break;
            }
        if (dataset == null)
            throw new IOException("no " + EXTENSION + " file in the zip");
        for (Map.Entry<String, byte[]> e : entries.entrySet())
            if (e.getKey().startsWith(folder) && !e.getKey().toLowerCase().endsWith(EXTENSION))
                dataset.putFile(e.getKey().substring(folder.length()), e.getValue());
        return dataset;
    }

    static boolean isZip(byte[] d)
    {
        return d.length > 4 && d[0] == 'P' && d[1] == 'K' && d[2] == 3 && d[3] == 4;
    }

    /** A zip's files (not folders), by path with "/" separators. */
    static Map<String, byte[]> unzip(byte[] d) throws IOException
    {
        Map<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(d));
        try
        {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null)
                if (!e.isDirectory())
                    out.put(e.getName().replace('\\', '/'), readAll(zip));
        }
        finally
        {
            zip.close();
        }
        return out;
    }

    private static byte[] readAll(InputStream in) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0)
            out.write(buffer, 0, n);
        return out.toByteArray();
    }

    /** UTF-8 text without a byte order mark; Latin-1 if it is not valid UTF-8 (older editors' files). */
    static String text(byte[] d)
    {
        int start = d.length >= 3 && (d[0] & 0xFF) == 0xEF && (d[1] & 0xFF) == 0xBB && (d[2] & 0xFF) == 0xBF ? 3 : 0;
        try
        {
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(d, start, d.length - start)).toString();
        }
        catch (java.nio.charset.CharacterCodingException e)
        {
            return new String(d, start, d.length - start, StandardCharsets.ISO_8859_1);
        }
    }

    // --- values -------------------------------------------------------------

    /** A whole number, or null if absent or not a number. */
    public Integer getInt(String section, String key)
    {
        String v = get(section, key);
        if (v == null)
            return null;
        try
        {
            return Integer.valueOf(v.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /** A comma-separated list of whole numbers, or null if absent or any is not a number. */
    public int[] getInts(String section, String key)
    {
        String v = get(section, key);
        if (v == null)
            return null;
        String[] parts = v.split(",");
        int[] out = new int[parts.length];
        try
        {
            for (int i = 0; i < parts.length; i++)
                out[i] = Integer.parseInt(parts[i].trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        return out;
    }

    public static String join(int[] values)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++)
            sb.append(i == 0 ? "" : ", ").append(values[i]);
        return sb.toString();
    }

    /** yes/no, or null. */
    public Boolean getBoolean(String section, String key)
    {
        String v = get(section, key);
        if (v == null)
            return null;
        v = v.trim().toLowerCase();
        if (v.equals("yes") || v.equals("true") || v.equals("1") || v.equals("on"))
            return Boolean.TRUE;
        if (v.equals("no") || v.equals("false") || v.equals("0") || v.equals("off"))
            return Boolean.FALSE;
        return null;
    }

    public static String yesNo(boolean b)
    {
        return b ? "yes" : "no";
    }
}
