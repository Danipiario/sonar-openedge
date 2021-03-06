/********************************************************************************
 * Copyright (c) 2003-2015 John Green
 * Copyright (c) 2015-2018 Riverside Software
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU Lesser General Public License v3.0
 * which is available at https://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-3.0
 ********************************************************************************/
package org.prorefactor.core.unittest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.prorefactor.core.ABLNodeType;
import org.prorefactor.core.IConstants;
import org.prorefactor.core.JPNode;
import org.prorefactor.core.JsonNodeLister;
import org.prorefactor.core.unittest.util.UnitTestModule;
import org.prorefactor.proparse.ProParserTokenTypes;
import org.prorefactor.refactor.RefactorSession;
import org.prorefactor.treeparser.ParseUnit;
import org.prorefactor.treeparser.symbols.TableBuffer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import antlr.ANTLRException;
import antlr.Token;
import antlr.TokenStream;
import eu.rssw.pct.RCodeInfo;
import eu.rssw.pct.RCodeInfo.InvalidRCodeException;

/**
 * Test the tree parsers against problematic syntax. These tests just run the tree parsers against the data/bugsfixed
 * directory. If no exceptions are thrown, then the tests pass. The files in the "bugsfixed" directories are subject to
 * change, so no other tests should be added other than the expectation that they parse clean.
 */
public class BugFixTest {
  private final static String SRC_DIR = "src/test/resources/data/bugsfixed";
  private final static String TEMP_DIR = "target/nodes-lister/data/bugsfixed";

  private RefactorSession session;
  private File tempDir = new File(TEMP_DIR);

  private List<String> jsonOut = new ArrayList<>();
  private List<String> jsonNames = new ArrayList<>();

  @BeforeTest
  public void setUp() throws IOException, InvalidRCodeException {
    Injector injector = Guice.createInjector(new UnitTestModule());
    session = injector.getInstance(RefactorSession.class);
    session.getSchema().createAlias("foo", "sports2000");
    session.injectTypeInfo(
        new RCodeInfo(new FileInputStream("src/test/resources/data/rssw/pct/ParentClass.r")).getTypeInfo());
    session.injectTypeInfo(
        new RCodeInfo(new FileInputStream("src/test/resources/data/rssw/pct/ChildClass.r")).getTypeInfo());
    session.injectTypeInfo(
        new RCodeInfo(new FileInputStream("src/test/resources/data/ttClass.r")).getTypeInfo());
    session.injectTypeInfo(
        new RCodeInfo(new FileInputStream("src/test/resources/data/ProtectedTT.r")).getTypeInfo());

    tempDir.mkdirs();
  }

  @AfterTest
  public void tearDown() throws IOException {
    PrintWriter writer = new PrintWriter(new File(tempDir, "index.html"));
    writer.println("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"stylesheet\" type=\"text/css\" href=\"http://riverside-software.fr/d3-style.css\" />");
    writer.println("<script src=\"http://riverside-software.fr/jquery-1.10.2.min.js\"></script><script src=\"http://riverside-software.fr/d3.v3.min.js\"></script>");
    writer.println("<script>var data= { \"files\": [");
    int zz = 1;
    for (String str : jsonNames) {
      if (zz > 1) {
        writer.write(',');
      }
      writer.print("{ \"file\": \"" + str + "\", \"var\": \"json" + zz++ + "\" }");
    }
    writer.println("]};");
    zz = 1;
    for (String str : jsonOut) {
      writer.println("var json" + zz++ + " = " + str + ";");
    }
    writer.println("</script></head><body><div id=\"wrapper\"><div id=\"left\"></div><div id=\"tree-container\"></div></div>");
    writer.println("<script src=\"http://riverside-software.fr/dndTreeDebug.js\"></script></body></html>");
    writer.close();
  }

  private ParseUnit genericTest(String file) throws ANTLRException {
    ParseUnit pu = new ParseUnit(new File(SRC_DIR, file), session);
    assertNull(pu.getTopNode());
    assertNull(pu.getRootScope());
    pu.parse();
    pu.treeParser01();
    assertNotNull(pu.getTopNode());
    assertNotNull(pu.getRootScope());

    StringWriter writer = new StringWriter();
    JsonNodeLister nodeLister = new JsonNodeLister(pu.getTopNode(), writer, ABLNodeType.LEFTPAREN,
        ABLNodeType.RIGHTPAREN, ABLNodeType.COMMA, ABLNodeType.PERIOD, ABLNodeType.LEXCOLON, ABLNodeType.OBJCOLON,
        ABLNodeType.THEN, ABLNodeType.END);
    nodeLister.print();
    
    jsonNames.add(file);
    jsonOut.add(writer.toString());

    return pu;
  }

  private TokenStream genericLex(String file) throws ANTLRException {
    ParseUnit pu = new ParseUnit(new File(SRC_DIR, file), session);
    assertNull(pu.getTopNode());
    assertNull(pu.getRootScope());
    assertNull(pu.getMetrics());
    pu.lexAndGenerateMetrics();
    assertNotNull(pu.getMetrics());
    return pu.lex();
  }

  @Test
  public void testVarUsage() throws ANTLRException {
    ParseUnit unit = genericTest("varusage.cls");
    assertEquals(unit.getRootScope().getVariable("x1").getNumWrites(), 2);
    assertEquals(unit.getRootScope().getVariable("x1").getNumReads(), 1);
    assertEquals(unit.getRootScope().getVariable("x2").getNumWrites(), 1);
    assertEquals(unit.getRootScope().getVariable("x2").getNumReads(), 2);
    assertEquals(unit.getRootScope().getVariable("x3").getNumWrites(), 1);
    assertEquals(unit.getRootScope().getVariable("x3").getNumReads(), 0);
    assertEquals(unit.getRootScope().getVariable("x4").getNumReads(), 1);
    assertEquals(unit.getRootScope().getVariable("x4").getNumWrites(), 0);

    assertEquals(unit.getRootScope().getVariable("lProcedure1").getNumReads(), 1);
    assertEquals(unit.getRootScope().getVariable("lProcedure1").getNumWrites(), 0);
    assertEquals(unit.getRootScope().getVariable("lProcedure2").getNumReads(), 1);
    assertEquals(unit.getRootScope().getVariable("lProcedure2").getNumWrites(), 0);
    assertEquals(unit.getRootScope().getVariable("lApsv").getNumReads(), 1);
    assertEquals(unit.getRootScope().getVariable("lApsv").getNumWrites(), 0);
    assertEquals(unit.getRootScope().getVariable("lRun").getNumReads(), 0);
    assertEquals(unit.getRootScope().getVariable("lRun").getNumWrites(), 1);
  }

  @Test
  public void test01() throws ANTLRException {
    genericTest("bug01.p");
  }

  @Test
  public void test02() throws ANTLRException {
    genericTest("bug02.p");
  }

  @Test
  public void test03() throws ANTLRException {
    genericTest("bug03.p");
  }

  @Test
  public void test04() throws ANTLRException {
    genericTest("bug04.p");
  }

  @Test
  public void test05() throws ANTLRException {
    genericTest("bug05.p");
  }

  @Test
  public void test06() throws ANTLRException {
    genericTest("bug06.p");
  }

  @Test
  public void test07() throws ANTLRException {
    genericTest("interface07.cls");
  }

  @Test
  public void test08() throws ANTLRException {
    genericTest("bug08.cls");
  }

  @Test
  public void test09() throws ANTLRException {
    genericTest("bug09.p");
  }

  @Test
  public void test10() throws ANTLRException {
    genericTest("bug10.p");
  }

  @Test
  public void test11() throws ANTLRException {
    genericTest("bug11.p");
  }

  @Test
  public void test12() throws ANTLRException {
    genericTest("bug12.p");
  }

  @Test
  public void test13() throws ANTLRException {
    genericTest("bug13.p");
  }

  @Test
  public void test14() throws ANTLRException {
    genericTest("bug14.p");
  }

  @Test
  public void test15() throws ANTLRException {
    genericTest("bug15.p");
  }

  @Test
  public void test16() throws ANTLRException {
    genericTest("bug16.p");
  }

  @Test
  public void test17() throws ANTLRException {
    genericTest("bug17.p");
  }

  @Test
  public void test18() throws ANTLRException {
    genericTest("bug18.p");
  }

  @Test
  public void test19() throws ANTLRException {
    genericTest("bug19.p");
  }

  @Test
  public void test20() throws ANTLRException {
    genericTest("bug20.p");
  }

  @Test
  public void test21() throws ANTLRException {
    genericTest("bug21.cls");
  }

  @Test
  public void test22() throws ANTLRException {
    genericTest("bug22.cls");
  }

  @Test
  public void test23() throws ANTLRException {
    genericTest("bug23.cls");
  }

  @Test
  public void test24() throws ANTLRException {
    genericTest("bug24.p");
  }

  @Test
  public void test25() throws ANTLRException {
    genericTest("bug25.p");
  }

  @Test
  public void test26() throws ANTLRException {
    genericTest("bug26.cls");
  }

  @Test
  public void test27() throws ANTLRException {
    genericTest("bug27.cls");
  }

  @Test
  public void test28() throws ANTLRException {
    genericTest("bug28.cls");
  }

  @Test
  public void test29() throws ANTLRException {
    genericTest("bug29.p");
  }

  @Test
  public void test30() throws ANTLRException {
    genericTest("bug30.p");
  }

  @Test
  public void test31() throws ANTLRException {
    genericTest("bug31.cls");
  }

  @Test
  public void test32() throws ANTLRException {
    genericLex("bug32.i");
  }

  @Test
  public void test33() throws ANTLRException {
    genericTest("bug33.cls");
  }

  @Test
  public void test34() throws ANTLRException {
    genericTest("bug34.p");
  }

  @Test
  public void test35() throws ANTLRException {
    genericTest("bug35.p");
  }

  @Test
  public void test36() throws ANTLRException {
    genericTest("bug36.p");
  }

  // Next two tests : same exception should be thrown in both cases
//  @Test(expectedExceptions = {ProparseRuntimeException.class})
//  public void testCache1() throws ANTLRException {
//    genericTest("CacheChild.cls");
//  }
//
//  @Test(expectedExceptions = {ProparseRuntimeException.class})
//  public void testCache2() throws ANTLRException {
//    genericTest("CacheChild.cls");
//  }

  @Test
  public void testSaxWriter() throws ANTLRException {
    genericTest("sax-writer.p");
  }

  @Test
  public void testNoBox() throws ANTLRException {
    genericTest("nobox.p");
  }

  @Test
  public void testOnStatement() throws ANTLRException {
    genericTest("on_statement.p");
  }

  @Test
  public void testIncludeInComment() throws ANTLRException {
    genericTest("include_comment.p");
  }

  @Test
  public void testCreateComObject() throws ANTLRException {
    ParseUnit unit = genericTest("createComObject.p");
    List<JPNode> list = unit.getTopNode().query(ABLNodeType.CREATE);
    // COM automation
    assertEquals(list.get(0).getLine(), 3);
    assertEquals(list.get(0).getState2(), ProParserTokenTypes.Automationobject);
    assertEquals(list.get(1).getLine(), 4);
    assertEquals(list.get(1).getState2(), ProParserTokenTypes.Automationobject);
    // Widgets
    assertEquals(list.get(2).getLine(), 8);
    assertEquals(list.get(2).getState2(), ProParserTokenTypes.WIDGET);
    assertEquals(list.get(3).getLine(), 12);
    assertEquals(list.get(3).getState2(), ProParserTokenTypes.WIDGET);
    // Ambiguous
    assertEquals(list.get(4).getLine(), 15);
    assertEquals(list.get(4).getState2(), ProParserTokenTypes.WIDGET);
  }

  @Test
  public void testCopyLob() throws ANTLRException {
    genericTest("copylob.p");
  }

  @Test
  public void testOsCreate() throws ANTLRException {
    genericTest("oscreate.p");
  }

  @Test
  public void testGetDbClient() throws ANTLRException {
    genericTest("getdbclient.p");
  }

  @Test
  public void testDoubleColon() throws ANTLRException {
    genericTest("double-colon.p");
  }

  @Test
  public void testTildeInComment() throws ANTLRException {
    TokenStream stream = genericLex("comment-tilde.p");
    Token tok = stream.nextToken();
    assertEquals(tok.getType(), ProParserTokenTypes.COMMENT);
    assertEquals(tok.getText(), "// \"~n\"");
    assertEquals(stream.nextToken().getType(), ProParserTokenTypes.WS);
    assertEquals(stream.nextToken().getType(), ProParserTokenTypes.DEFINE);
  }

  @Test
  public void testTildeInComment2() throws ANTLRException {
    TokenStream stream = genericLex("comment-tilde2.p");
    assertEquals(stream.nextToken().getType(), ProParserTokenTypes.DEFINE);
    assertEquals(stream.nextToken().getType(), ProParserTokenTypes.WS);
    Token tok = stream.nextToken();
    assertEquals(tok.getType(), ProParserTokenTypes.COMMENT);
    assertEquals(tok.getText(), "// \"~n\"");
  }

  @Test(enabled = false, description = "Issue #309, won't fix,")
  public void testAbstractKw() throws ANTLRException {
    genericTest("abstractkw.p");
  }

  @Test
  public void testNoArgFunc() throws ANTLRException {
    ParseUnit pu = genericTest("noargfunc.p");
    List<JPNode> nodes = pu.getTopNode().query(ABLNodeType.MESSAGE);
    assertEquals(nodes.get(0).getFirstChild().getFirstChild().getNodeType(), ABLNodeType.GUID);
    assertEquals(nodes.get(1).getFirstChild().getFirstChild().getNodeType(), ABLNodeType.FIELD_REF);
    assertEquals(nodes.get(2).getFirstChild().getFirstChild().getNodeType(), ABLNodeType.TIMEZONE);
    assertEquals(nodes.get(3).getFirstChild().getFirstChild().getNodeType(), ABLNodeType.FIELD_REF);
    assertEquals(nodes.get(4).getFirstChild().getFirstChild().getNodeType(), ABLNodeType.MTIME);
    assertEquals(nodes.get(5).getFirstChild().getFirstChild().getNodeType(), ABLNodeType.FIELD_REF);
  }

  @Test
  public void testLexer01() throws ANTLRException {
    @SuppressWarnings("unused")
    TokenStream stream = genericLex("lex.p");
  }

  @Test
  public void testDataset() throws ANTLRException {
    genericTest("DatasetParentFields.p");
  }

  @Test
  public void testExtentFunction() throws ANTLRException {
    genericTest("testextent1.cls");
    genericTest("testextent2.p");
  }

  @Test
  public void testTTLikeDB01() throws ANTLRException {
    genericTest("ttlikedb01.p");
  }

  @Test
  public void testStopAfter() throws ANTLRException {
    genericTest("stopafter.p");
  }

  @Test
  public void testTTLikeDB02() throws ANTLRException {
    ParseUnit unit = new ParseUnit(new File("src/test/resources/data/bugsfixed/ttlikedb02.p"), session);
    assertNull(unit.getTopNode());
    assertNull(unit.getRootScope());
    unit.treeParser01();
    assertNotNull(unit.getTopNode());

    // First FIND statement
    JPNode node = unit.getTopNode().queryStateHead(ABLNodeType.FIND).get(0);
    assertNotNull(node);
    assertEquals(node.query(ABLNodeType.RECORD_NAME).size(), 1);
    Object obj = node.query(ABLNodeType.RECORD_NAME).get(0).getLink(IConstants.SYMBOL);
    assertNotNull(obj);
    assertEquals(((TableBuffer) obj).getTable().getStoretype(), IConstants.ST_DBTABLE);

    // Second FIND statement
    node = unit.getTopNode().queryStateHead(ABLNodeType.FIND).get(1);
    assertNotNull(node);
    assertEquals(node.query(ABLNodeType.RECORD_NAME).size(), 1);
    obj = node.query(ABLNodeType.RECORD_NAME).get(0).getLink(IConstants.SYMBOL);
    assertNotNull(obj);
    assertEquals(((TableBuffer) obj).getTable().getStoretype(), IConstants.ST_TTABLE);

    // Third FIND statement
    node = unit.getTopNode().queryStateHead(ABLNodeType.FIND).get(2);
    assertNotNull(node);
    assertEquals(node.query(ABLNodeType.RECORD_NAME).size(), 1);
    obj = node.query(ABLNodeType.RECORD_NAME).get(0).getLink(IConstants.SYMBOL);
    assertNotNull(obj);
    assertEquals(((TableBuffer) obj).getTable().getStoretype(), IConstants.ST_DBTABLE);

    // Fourth FIND statement
    node = unit.getTopNode().queryStateHead(ABLNodeType.FIND).get(3);
    assertNotNull(node);
    assertEquals(node.query(ABLNodeType.RECORD_NAME).size(), 1);
    obj = node.query(ABLNodeType.RECORD_NAME).get(0).getLink(IConstants.SYMBOL);
    assertNotNull(obj);
    assertEquals(((TableBuffer) obj).getTable().getStoretype(), IConstants.ST_TTABLE);
  }

  @Test
  public void testRCodeStructure() throws ANTLRException {
     ParseUnit unit = new ParseUnit(new File("src/test/resources/data/rssw/pct/ChildClass.cls"), session);
     assertNull(unit.getTopNode());
     assertNull(unit.getRootScope());
     unit.treeParser01();
     assertNotNull(unit.getTopNode());
   }

  @Test
  public void testProtectedTTAndBuffers() throws ANTLRException {
     ParseUnit unit = new ParseUnit(new File("src/test/resources/data/ProtectedTT.cls"), session);
     assertNull(unit.getTopNode());
     assertNull(unit.getRootScope());
     unit.treeParser01();
     assertNotNull(unit.getTopNode());
   }

  @Test
  public void testAscendingFunction() throws ANTLRException {
    ParseUnit unit = new ParseUnit(new File("src/test/resources/data/bugsfixed/ascending.p"), session);
    assertNull(unit.getTopNode());
    assertNull(unit.getRootScope());
    unit.treeParser01();
    assertNotNull(unit.getTopNode());

    // Message statement
    JPNode node = unit.getTopNode().queryStateHead(ABLNodeType.MESSAGE).get(0);
    assertNotNull(node);
    assertEquals(node.query(ABLNodeType.ASCENDING).size(), 0);
    assertEquals(node.query(ABLNodeType.ASC).size(), 1);

    // Define TT statement
    JPNode node2 = unit.getTopNode().queryStateHead(ABLNodeType.DEFINE).get(0);
    assertNotNull(node2);
    assertEquals(node2.query(ABLNodeType.ASCENDING).size(), 1);
    assertEquals(node2.query(ABLNodeType.ASC).size(), 0);
  }

  @Test(enabled = false, description = "Issue #356, won't fix,")
  public void testDefineMenu() throws ANTLRException {
    genericTest("definemenu.p");
  }

  @Test
  public void testOptionsField() throws ANTLRException {
    genericTest("options_field.p");
  }

}
