package com.std4453.jbc.lexical;

import java.util.HashMap;
import java.util.Map;

import com.std4453.jbc.util.DataPool;
import com.std4453.jbc.util.DataPool.DataPoolInterval;
import com.std4453.jbc.util.Pair;

public class Regexp {
	private static final DataPool<Regexp> regexpPool = new DataPool<Regexp>(
			Regexp.class, 8, 5, 32);
	private static final Map<Character, Character> escapedCharacters = new HashMap<Character, Character>();

	static {
		escapedCharacters.put('\\', '\\');
		escapedCharacters.put('?', '?');
		escapedCharacters.put('+', '+');
		escapedCharacters.put('*', '*');
		escapedCharacters.put('(', '(');
		escapedCharacters.put(')', ')');
		escapedCharacters.put('n', '\n');
		escapedCharacters.put('t', '\t');
	}

	private static enum Rule {
		LEAF, OR, AND, HAS_OR_NOT, MORE_THAN_ZERO, ANY_OR_NONE;
	}

	protected char availableChar;
	protected DataPoolInterval<Regexp> children;

	protected Rule rule;

	private Regexp(char availableChar) {
		this.rule = Rule.LEAF;
		this.availableChar = availableChar;
		this.children = null;
	}

	private Regexp() {
		this.children = regexpPool.allocate(0);
	}

	public char getAvailableChar() {
		return availableChar;
	}

	public void setAvailableChar(char availableChar) {
		this.availableChar = availableChar;
	}

	public DataPoolInterval<Regexp> getChildren() {
		return children;
	}

	public void setChildren(DataPoolInterval<Regexp> children) {
		this.children = children;
	}

	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	private Regexp(Rule rule) {
		this.rule = rule;
		this.children = regexpPool.allocate(0);
	}

	private void addChild(Regexp child) {
		regexpPool.allocateMore(children, 1);
		children.setElement(children.count() - 1, child);
	}

	public boolean matches(String input) {
		return matches(input, 0) == input.length();
	}

	public String matchPartly(String input) {
		int matched = matches(input, 0);
		if (matched < 0)
			return "";
		return input.substring(0, matched);
	}

	public int matches(String input, int index) {
		switch (this.rule) {
		case ANY_OR_NONE:
			if (this.children.count() < 1)
				return -1;
			int matched = this.children.getElement(0).matches(input, index);
			if (matched < 0)
				return 0;
			else {
				int pointer = index + matched;
				while (matched >= 0) {
					matched = this.children.getElement(0).matches(input,
							pointer);
					if (matched >= 0)
						pointer += matched;
				}
				return pointer - index;
			}
		case AND:
			int pointer = index;
			for (Regexp regexp : this.children) {
				matched = regexp.matches(input, pointer);
				if (matched < 0)
					return -1;
				else
					pointer += matched;
			}
			return pointer - index;
		case HAS_OR_NOT:
			if (this.children.count() < 1)
				return -1;
			matched = this.children.getElement(0).matches(input, index);
			if (matched < 0)
				return 0;
			else
				return matched;
		case LEAF:
			return input.length() > index ? availableChar == 0 ? 0 : input
					.charAt(index) == availableChar ? 1 : -1 : -1;
		case MORE_THAN_ZERO:
			if (this.children.count() < 1)
				return -1;
			matched = this.children.getElement(0).matches(input, index);
			if (matched < 0)
				return -1;
			else {
				pointer = index + matched;
				while (matched >= 0) {
					matched = this.children.getElement(0).matches(input,
							pointer);
					if (matched >= 0)
						pointer += matched;
				}
				return pointer - index;
			}
		case OR:
			int max = -1;
			for (Regexp regexp : this.children) {
				matched = regexp.matches(input, index);
				if (matched >= 0 && matched > max)
					max = matched;
			}
			return max;
		default:
			return -1;
		}
	}

	public static Regexp compile(String input) {
		return compile(input, 0, false).getLeft();
	}

	private static Pair<Regexp, Integer> compile(String input, int index,
			boolean fromBracket) {
		Regexp data = new Regexp();
		Regexp ret = new Regexp();
		boolean afterSlash = false;
		int right = 0;
		for (int i = index; i < input.length(); ++i) {
			char ch = input.charAt(i);
			if (ch == '\\') {
				afterSlash = true;
			} else if (ch == '(' && !afterSlash) {
				Pair<Regexp, Integer> pair = compile(input, i + 1, true);
				data.addChild(pair.getLeft());
				i = pair.getRight();
			} else if (ch == ')' && !afterSlash) {
				if (fromBracket) {
					right = i;
					break;
				} else {
					logError(input, i, "Unexpected '('!");
				}
			} else if (ch == '?' && !afterSlash) {
				DataPoolInterval<Regexp> rootChildren = data.getChildren();
				if (rootChildren.count() < 1) {
					logError(input, i, "'?' must be after a token!");
				}
				Regexp tmp = new Regexp(Rule.HAS_OR_NOT);
				tmp.addChild(rootChildren.getElement(rootChildren.count() - 1));
				rootChildren.setElement(rootChildren.count() - 1, tmp);
			} else if (ch == '*' && !afterSlash) {
				DataPoolInterval<Regexp> rootChildren = data.getChildren();
				if (rootChildren.count() < 1) {
					logError(input, i, "'*' must be after a token!");
				}
				Regexp tmp = new Regexp(Rule.ANY_OR_NONE);
				tmp.addChild(rootChildren.getElement(rootChildren.count() - 1));
				rootChildren.setElement(rootChildren.count() - 1, tmp);
			} else if (ch == '+' && !afterSlash) {
				DataPoolInterval<Regexp> rootChildren = data.getChildren();
				if (rootChildren.count() < 1) {
					logError(input, i, "'+' must be after a token!");
				}
				Regexp tmp = new Regexp(Rule.MORE_THAN_ZERO);
				tmp.addChild(rootChildren.getElement(rootChildren.count() - 1));
				rootChildren.setElement(rootChildren.count() - 1, tmp);
			} else if (ch == '|' && !afterSlash) {
				DataPoolInterval<Regexp> rootChildren = data.getChildren();
				if (rootChildren.count() < 1) {
					logError(input, i, "'|' must be after a token!");
				}

				if (rootChildren.count() == 1) {
					ret.addChild(rootChildren.getElement(0));
				} else {
					Regexp tmp = new Regexp(Rule.AND);
					for (Regexp regexp : rootChildren)
						tmp.addChild(regexp);
					ret.addChild(tmp);
				}
				rootChildren.setEnd(rootChildren.getStart());
			} else if (afterSlash) {
				if (escapedCharacters.containsKey(ch)) {
					data.addChild(new Regexp(escapedCharacters.get(ch)));
					afterSlash = false;
				} else {
					logError(input, i, "Escape character \"\\" + ch
							+ "\" is not allowed!");
				}
			} else {
				data.addChild(new Regexp(ch));
			}
		}
		if (ret.getChildren().count() > 0) {
			if (data.getChildren().count() < 1) {
				logError(input, input.length(), "Expected token(s) after '|'!");
			}
			if (data.getChildren().count() > 1) {
				Regexp tmp = new Regexp(Rule.AND);
				for (Regexp regexp : data.getChildren())
					tmp.addChild(regexp);
				ret.addChild(tmp);
			} else {
				ret.addChild(data.getChildren().getElement(0));
			}
			ret.setRule(Rule.OR);
			data.getChildren().setEnd(data.getChildren().getStart());
		} else {
			if (data.getChildren().count() < 1) {
				logError(input, input.length(), "Input length can't be zero!");
			}
			ret.setRule(Rule.AND);
			for (Regexp regexp : data.getChildren())
				ret.addChild(regexp);
			data.getChildren().setEnd(data.getChildren().getStart());
		}
		data.release();
		return new Pair<>(ret, right);
	}

	private static void logError(String input, int index, String msg) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < index; ++i)
			sb.append(" ");
		sb.append("^");
		throw new RegexpParseException(String.format(
				"Parse Exception near character '%s' near index %d:%s\n%s\n%s",
				input.charAt(index), index, msg, input, sb.toString()));
	}

	public void release() {
		if (this.children != null) {
			for (Regexp regexp : children)
				regexp.release();
			regexpPool.release(children);
		}
	}

	public static void main(String[] args) {
		String pattern = "a(a+|(b*a?bb)\\)?)";
		String[] tests = new String[] { "abbbbbbbabb)", "abbbbbbbbbbbb", "aa",
				"aaa", "aaaaaa", "aabb", "abb)","aa)",")","ababbb" };
		System.out.println("The pattern is " + pattern);
		System.out.println();
		System.out.println("The compiles pattern is:");
		Regexp regexp = compile(pattern);
		System.out.println(regexp);
		System.out.println();
		System.out.println("Matching tests:");
		for (String test : tests) {
			System.out.println(test
					+ (regexp.matches(test) ? " matches" : " doesn't match")
					+ " with " + pattern);
		}
		regexp.release();
	}

	public String toString() {
		return toString(new StringBuilder()).toString();
	}

	private StringBuilder toString(StringBuilder sb) {
		return toString(sb, 0);
	}

	private StringBuilder toString(StringBuilder sb, int level) {
		for (int i = 0; i < level; ++i)
			sb.append("\t");
		sb.append("Regexp{rule=Rule." + rule);
		if (rule == Rule.LEAF)
			sb.append(", ch=" + availableChar + " (0x"
					+ Integer.toHexString(availableChar) + ")}");
		else {
			sb.append(", children=[\n");
			for (Regexp regexp : this.children) {
				regexp.toString(sb, level + 1);
				if (regexp != this.children
						.getElement(this.children.count() - 1))
					sb.append(",");
				sb.append("\n");
			}
			for (int i = 0; i < level; ++i)
				sb.append("\t");
			sb.append("]}");
		}
		return sb;
	}
}