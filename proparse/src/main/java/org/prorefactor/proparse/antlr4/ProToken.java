/********************************************************************************
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
package org.prorefactor.proparse.antlr4;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.WritableToken;
import org.prorefactor.core.ABLNodeType;

public class ProToken implements WritableToken {
  private static final String INVALID_TYPE = "Invalid type number ";

  /**
   * This is the backing field for {@link #getType} and {@link #setType}.
   */
  private ABLNodeType type;

  /**
   * This is the backing field for {@link #getLine} and {@link #setLine}.
   */
  private int line;

  /**
   * This is the backing field for {@link #getCharPositionInLine} and {@link #setCharPositionInLine}.
   */
  private int charPositionInLine = -1; // set to invalid position

  /**
   * This is the backing field for {@link #getChannel} and {@link #setChannel}.
   */
  private int channel = DEFAULT_CHANNEL;

  /**
   * This is the backing field for {@link #getText} when the token text is explicitly set in the constructor or via
   * {@link #setText}.
   *
   * @see #getText()
   */
  private String text;

  /**
   * This is the backing field for {@link #getTokenIndex} and {@link #setTokenIndex}.
   */
  private int index = -1;

  /**
   * This is the backing field for {@link #getStartIndex} and {@link #setStartIndex}.
   */
  private int start;

  /**
   * This is the backing field for {@link #getStopIndex} and {@link #setStopIndex}.
   */
  private int stop;

  private int fileIndex;
  private int endFileIndex;
  private int endLine;
  private int endCharPositionInLine;
  private int macroSourceNum;

  private String analyzeSuspend = null;
  private ProToken hiddenBefore = null;
  private boolean macroExpansion;

  public ProToken(ABLNodeType type, String text) {
    this.type = type;
    this.channel = DEFAULT_CHANNEL;
    this.text = text;
    this.fileIndex = 0;
    this.charPositionInLine = 0;
  }

  public ProToken(int type, int channel, int start, int stop, int line, int col) {
    this.type = ABLNodeType.getNodeType(type);
    if (this.type == null)
      throw new IllegalArgumentException(INVALID_TYPE + type);
    this.channel = channel;
    this.start = start;
    this.stop = stop;
    this.line = line;
    this.charPositionInLine = col;
  }

  public int getEndFileIndex() {
    return endFileIndex;
  }

  public void setEndFileIndex(int endFileIndex) {
    this.endFileIndex = endFileIndex;
  }

  public int getEndLine() {
    return endLine;
  }

  public void setEndLine(int endLine) {
    this.endLine = endLine;
  }

  public int getEndCharPositionInLine() {
    return endCharPositionInLine;
  }

  public void setEndCharPositionInLine(int endCharPositionInLine) {
    this.endCharPositionInLine = endCharPositionInLine;
  }

  public int getMacroSourceNum() {
    return macroSourceNum;
  }

  public void setMacroSourceNum(int macroSourceNum) {
    this.macroSourceNum = macroSourceNum;
  }

  public int getFileIndex() {
    return fileIndex;
  }

  public void setFileIndex(int fileIndex) {
    this.fileIndex = fileIndex;
  }

  public void setAnalyzeSuspend(String analyzeSuspend) {
    this.analyzeSuspend = analyzeSuspend;
  }

  public void setMacroExpansion(boolean macroExpansion) {
    this.macroExpansion = macroExpansion;
  }

  /**
   * Returns true if last character of token was generated from a macro expansion, i.e. {&amp;SOMETHING}.
   * This doesn't mean that all characters were generated from a macro, e.g. {&prefix}VarName will return false 
   */
  public boolean isMacroExpansion() {
    return macroExpansion;
  }

  /**
   * @return Comma-separated list of &amp;ANALYZE-SUSPEND options. Null for code not managed by AppBuilder.
   */
  public String getAnalyzeSuspend() {
    return analyzeSuspend;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public int getType() {
    return type.getType();
  }

  public ABLNodeType getNodeType() {
    return type;
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public int getCharPositionInLine() {
    return charPositionInLine;
  }

  @Override
  public int getChannel() {
    return channel;
  }

  @Override
  public int getTokenIndex() {
    return index;
  }

  @Override
  public int getStartIndex() {
    return start;
  }

  @Override
  public int getStopIndex() {
    return stop;
  }

  @Override
  public TokenSource getTokenSource() {
    return null;
  }

  @Override
  public CharStream getInputStream() {
    return null;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }

  @Override
  public void setType(int type) {
    this.type = ABLNodeType.getNodeType(type);
    if (this.type == null)
      throw new IllegalArgumentException(INVALID_TYPE + type);
  }

  public void setNodeType(ABLNodeType type) {
    if (type == null)
      throw new IllegalArgumentException(INVALID_TYPE + type);
    this.type = type;
  }

  @Override
  public void setLine(int line) {
    this.line = line;
  }

  @Override
  public void setCharPositionInLine(int pos) {
    this.charPositionInLine = pos;
  }

  @Override
  public void setChannel(int channel) {
    this.channel = channel;
  }

  @Override
  public void setTokenIndex(int index) {
    this.index = index;
  }

  public void setHiddenBefore(ProToken hiddenBefore) {
    this.hiddenBefore = hiddenBefore;
  }

  @Override
  public String toString() {
    return "[\"" + text.replace('\r', ' ').replace('\n', ' ') + "\",<" + type + ">,macro=" + macroSourceNum + ",start="
        + fileIndex + ":" + line + ":" + charPositionInLine + ",end=" + endFileIndex + ":" + endLine + ":"
        + endCharPositionInLine + "]";
  }
}
