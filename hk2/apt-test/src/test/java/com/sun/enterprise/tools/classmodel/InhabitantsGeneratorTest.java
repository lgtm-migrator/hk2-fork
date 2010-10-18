package com.sun.enterprise.tools.classmodel;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.glassfish.hk2.classmodel.reflect.AnnotationType;
import org.glassfish.hk2.classmodel.reflect.ParsingContext;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.InhabitantAnnotation;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.classmodel.ClassPath;
import org.jvnet.hk2.component.classmodel.InhabitantsParsingContextGenerator;

import com.sun.enterprise.tools.InhabitantsDescriptor;

/**
 * Tests the introspective type of InhabitantsGenerator.
 * 
 * @author Jeff Trent
 */
public class InhabitantsGeneratorTest {

//  @Ignore
  @Test
  public void sanityTest() throws Exception {
    ArrayList<File> testDir = getTestClassPathEntries(false);

    ClassPath classPath = ClassPath.create(null, testDir);
    InhabitantsGenerator generator = new InhabitantsGenerator(null, classPath, classPath);

    InhabitantsParsingContextGenerator ipcGen = generator.getContextGenerator();
    ParsingContext pc = ipcGen.getContext();
    assertNotNull(pc);
    
    Types types = pc.getTypes();
    
    AnnotationType ia = types.getBy(AnnotationType.class, InhabitantAnnotation.class.getName());
    AnnotationType s = types.getBy(AnnotationType.class, Service.class.getName());
    AnnotationType c = types.getBy(AnnotationType.class, Contract.class.getName());
    
    assertNotNull("@InhabitantAnnotation not found", ia);
    assertNotNull("Service not found", s);
    assertNotNull("@Contract not found", c);
  }
  
  /**
   * Another sanity type test
   */
//  @Ignore
  @Test
  public void autoDependsIsRequired() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(out, true);
    
    PrintStream old = System.err;
    Properties oldSysProps = System.getProperties();
    try {
      System.setErr(ps);
  //    ArrayList<File> testDir = getTestClassPathEntries();
      ClassPath classPath = ClassPath.create(null, (ArrayList<File>)null);

      // first, another sanity check
      InhabitantsGenerator generator = new InhabitantsGenerator(null, classPath, classPath);
  //    generator.add(testDir);
  
      InhabitantsParsingContextGenerator ipcGen = generator.getContextGenerator();
      ParsingContext pc = ipcGen.getContext();
      assertNotNull(pc);
      
      Types types = pc.getTypes();
      AnnotationType ia = types.getBy(AnnotationType.class, InhabitantAnnotation.class.getName());
      AnnotationType c = types.getBy(AnnotationType.class, Contract.class.getName());
      
      assertNull("@InhabitantAnnotation not found", ia);
      assertNull("@Contract not found", c);

      // real heart of test starts here
      File testDir = new File(new File("."), "target/test-classes");
      File outputFile = new File(testDir, "META-INF/inhabitants/default");
      
      System.setProperty(InhabitantsGenerator.PARAM_INHABITANT_FILE, outputFile.getAbsolutePath());
      System.setProperty(InhabitantsGenerator.PARAM_INHABITANTS_SOURCE_FILES, testDir.getAbsolutePath());
      System.setProperty(InhabitantsGenerator.PARAM_INHABITANTS_CLASSPATH, testDir.getAbsolutePath());
      InhabitantsGenerator.main(null);

      String errTxt = clean(out.toString());
      assertEquals("ERROR: HK2's auto-depends jar is an expected argument in " + 
          InhabitantsGenerator.PARAM_INHABITANTS_CLASSPATH + "\n", errTxt);
    } finally {
      System.setErr(old);
      System.clearProperty(InhabitantsGenerator.PARAM_INHABITANTS_CLASSPATH);
      System.setProperties(oldSysProps);
      ps.close();
    }
  }
  
  /**
   * Another sanity type test
   */
//  @Ignore
  @Test
  public void workingClassPathRecommended() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(out, true);
    
    PrintStream old = System.err;
    Properties oldSysProps = System.getProperties();
    try {
      System.setErr(ps);

      File testDir = new File(new File("."), "target/test-classes");
      File outputFile = new File(testDir, "META-INF/inhabitants/default");
      
      System.setProperty(InhabitantsGenerator.PARAM_INHABITANT_FILE, outputFile.getAbsolutePath());
      System.setProperty(InhabitantsGenerator.PARAM_INHABITANTS_SOURCE_FILES, toString(getTestClassPathEntries(false)));
//      System.setProperty(InhabitantsGenerator.PARAM_INHABITANTS_CLASSPATH, testDir.getAbsolutePath());
      InhabitantsGenerator.main(null);

      String errTxt = out.toString();
      assertTrue(errTxt + " is unexpected", errTxt.startsWith("WARNING: sysprop " + 
          InhabitantsGenerator.PARAM_INHABITANTS_CLASSPATH + 
          " is missing; defaulting to classpath="));
    } finally {
      System.setErr(old);
      System.setProperties(oldSysProps);
      ps.close();
    }
  }
  
  
  /**
   * auto-depends modules is required to be passed into the generator or else an error will occur.
   * 
   * if the "output" only contains code from <code>com/sun/enterprise/tools/classmodel/test/local</code> then
   * there is a problem in auto-depends filtering out jars with habitats during introspection.
   * test-inhabitant-generator.jar should also be present.
   * 
   * this test looks at the case where the classpath is only partially specified resulting in
   * a reduced view of the inhabitants.
   */
//  @Ignore
  @Test
  public void testReducedScopeHabitatFileGeneration() throws IOException {
    ArrayList<File> testDir = getTestClassPathEntries(false);

    InhabitantsDescriptor descriptor = new InhabitantsDescriptor();
    descriptor.enableDateOutput(false);
    
    ClassPath classPath = ClassPath.create(null, testDir);
    InhabitantsGenerator generator = new InhabitantsGenerator(descriptor, classPath, classPath);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(out);
    
    generator.generate(writer);
    writer.close();
    
    String output = clean(out.toString());
    assertNotNull(output);
    System.out.println("Output: \n" + output);
    System.out.println("Expected: \n" + expected(false));
    assertTrue("output (see javadoc comments): " + output, output.contains(expected(false)));
  }

  /**
   * this test, akin to the above, looks at the case where the classpath is fully specified
   * resulting in all of the correctly modeled inhabitants.
   */
//  @Ignore
  @Test
  public void testFullHabitatFileGeneration() throws IOException {
    ArrayList<File> inhabitantSources = getTestClassPathEntries(false);
    ArrayList<File> testingClassPath = getTestClassPathEntries(true);

    InhabitantsDescriptor descriptor = new InhabitantsDescriptor();
    descriptor.enableDateOutput(false);
    
    ClassPath inhabitantSourcesClassPath = ClassPath.create(null, inhabitantSources);
    ClassPath workingClassPath = ClassPath.create(null, testingClassPath);
    InhabitantsGenerator generator = new InhabitantsGenerator(descriptor, inhabitantSourcesClassPath, workingClassPath);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(out);
    
    generator.generate(writer);
    writer.close();
    
    String output = clean(out.toString());
    assertNotNull(output);
    System.out.println("Output: \n" + output);
    System.out.println("Expected: \n" + expected(true));
    assertTrue("output (see javadoc comments):\n" + output + "\nExpected:\n" + expected(true),
        output.contains(expected(true)));
  }

  String expected(boolean worldViewClassPath) {
    return expected(worldViewClassPath, true);
  }
  
  String expected(boolean worldViewClassPath, boolean fromClassModel) {
    StringBuilder sb = new StringBuilder();
    sb.append("class=com.sun.enterprise.tools.classmodel.test.BService,index=com.sun.enterprise.tools.classmodel.test.BContract\n");
    sb.append("class=com.sun.enterprise.tools.classmodel.test.RunLevelCloseableService,index=java.io.Closeable:closeable,index=org.jvnet.hk2.annotations.RunLevel\n");
    sb.append("class=com.sun.enterprise.tools.classmodel.test.AService,index=com.sun.enterprise.tools.classmodel.test.AContract:aservice,a=1,b=2\n");
    if (worldViewClassPath) {
      // world view classpath has full visibility so that class-model generates the true habitat
      sb.append("class=com.sun.enterprise.tools.classmodel.test.ServiceWithExternalContract,index=com.sun.enterprise.tools.classmodel.test.external.ExternalContract\n");
    } else {
      // without world-view, the external contracts in the inhabitants-gen-ifaces jar are not considered
      sb.append("class=com.sun.enterprise.tools.classmodel.test.ServiceWithExternalContract\n");
    }
    if (fromClassModel) {
      sb.append("class=com.sun.enterprise.tools.classmodel.test.local.LocalServiceInTestDir,index=java.io.Closeable\n");
    }
    sb.append("class=com.sun.enterprise.tools.classmodel.test.FactoryForCService,index=org.jvnet.hk2.annotations.FactoryFor:com.sun.enterprise.tools.classmodel.test.CService\n");
    sb.append("class=com.sun.enterprise.tools.classmodel.test.CService\n");
    if (worldViewClassPath) {
      sb.append("class=com.sun.enterprise.tools.classmodel.test.ServiceWithAbstractBaseHavingExternalContract,index=com.sun.enterprise.tools.classmodel.test.external.ExternalContract\n");
    } else {
      sb.append("class=com.sun.enterprise.tools.classmodel.test.ServiceWithAbstractBaseHavingExternalContract\n");
    }
    
    sb.append("class=test1.Start");
    return sb.toString();
  }

  /**
   * If {@link #testHabitatFileGeneration()} fails, then this guy will also always fail.
   */
//  @Ignore
  @Test
  public void testMain() throws Exception {
    File testDir = new File(new File("."), "target/test-classes");
    File outputFile = new File(testDir, "META-INF/inhabitants/default");
    outputFile.delete();
    
    System.setProperty(InhabitantsGenerator.PARAM_INHABITANT_FILE, outputFile.getAbsolutePath());
    System.setProperty(InhabitantsGenerator.PARAM_INHABITANTS_SOURCE_FILES, toString(getTestClassPathEntries(false)));
    System.setProperty(InhabitantsGenerator.PARAM_INHABITANTS_CLASSPATH, toString(getTestClassPathEntries(true)));
    InhabitantsGenerator.main(null);
    
    assertTrue("expect to find: " + outputFile, outputFile.exists());

    FileInputStream fis = new FileInputStream(outputFile);
    String val = toString(fis);
    fis.close();
    
    assertTrue(val + " was not found to contain:\n" + 
        expected(true), val.contains(expected(true)));
  }
  
  private String toString(InputStream is) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line;
    while (null != (line = reader.readLine())) {
      sb.append(line).append("\n");
    }
    return sb.toString();
  }
  
  /**
   * Compares APT generation to class-model, introspection generation. 
   * @throws IOException 
   */
  @Test
  public void testAgainstAptGenerator() throws IOException {
    ClassLoader cl = new URLClassLoader(toURL(getTestClassPathEntries(false)), new ClassLoader(null) {
      @Override
      protected URL findResource(String name) {
        return null;
      }
    });

    Enumeration<URL> en = cl.getResources("META-INF/inhabitants/default");
    int count = 0;
    while (en.hasMoreElements()) {
      URL url = en.nextElement();
      count++;
      
      InputStream is = (InputStream)url.getContent();
      String val = toString(is);
      is.close();

      boolean fromClassModelIntrospection = url.getPath().toString().contains("apt-test/target/test-classes/META-INF/inhabitants/default");
      String expected = expected(true, fromClassModelIntrospection);
      
      assertTrue("expected " + url + " to contain output:\n" + expected +
          "\nbut instead was:\n" + val, val.contains(expected));
    }
    
    assertEquals("inhabitants files found", 2, count);
  }
  
  private URL[] toURL(ArrayList<File> testClassPathEntries) throws IOException {
    URL urls[] = new URL[testClassPathEntries.size()];
    int i = 0;
    for (File file : testClassPathEntries) {
      urls[i++] = file.toURL();
    }
    return urls;
  }

  private String clean(String string) {
    return string.replace("\r", "");
  }

  private String toString(ArrayList<File> list) {
    StringBuilder sb = new StringBuilder();
    for (File file : list) {
      if (sb.length() > 0) {
        sb.append(File.pathSeparator);
      }
      sb.append(file.getAbsolutePath());
    }
    return sb.toString();
  }

  public ArrayList<File> getTestClassPathEntries(boolean worldView) {
    ArrayList<File> entries = new ArrayList<File>();
    
    ClassPath classpath = ClassPath.create(null, false);
    Set<String> cpSet = classpath.getEntries();
    for (String entry : cpSet) {
      if (false) {
        entries.add(new File(entry));
      } else {
        if (entry.contains("test-inhabitant-generator")) {
          entries.add(new File(entry));
        } else if (entry.contains("auto-depends-") 
            && !entry.contains("auto-depends-plugin")) {
          if (worldView) {
            entries.add(new File(entry));
          } else if (!entry.contains("test-inhabitant-gen-ifaces")) {
            entries.add(new File(entry));
          }
        } else if (entry.contains("test-classes")) {
          entries.add(new File(entry));
        }
      }
    }
    
    if (entries.isEmpty()) {
      throw new RuntimeException("can't find test-classes in " + cpSet);
    }

    System.out.println("test classpath for worldview=" + worldView + " is " + entries);
    
    return entries;
  }
}
