package com.algorand.utils;
import java.nio.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class ReadAndWrite{

public static String readFile(String filePath) 
{
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
    {
        stream.forEach(s -> contentBuilder.append(s).append("\n"));
    }
    catch (IOException e)
    {
        e.printStackTrace();
    }
    return contentBuilder.toString();
}

public static void writeFile(String filePath,String content) 
  throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
    writer.write(content);
     
    writer.close();
}

}