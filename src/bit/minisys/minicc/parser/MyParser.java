package bit.minisys.minicc.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.gui.TreeViewer;

import com.fasterxml.jackson.databind.ObjectMapper;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;

class ScannerToken{
	public String lexeme;
	public String type;
	public int 	  id;
	public int	  line;
	public int    column;
}

public class MyParser implements IMiniCCParser {

	private ArrayList<ScannerToken> tknList;
	private int tokenIndex;
	private ScannerToken nextToken;
	private ScannerToken token;
	
	@Override
	public String run(String iFile) throws Exception {
		System.out.println("Parsing...");

		String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_PARSER_OUTPUT_EXT;
		String tFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
		
		tknList = loadTokens(tFile);
		tokenIndex = 0;

		ASTNode root = program();
		
		
		String[] dummyStrs = new String[16];
		TreeViewer viewr = new TreeViewer(Arrays.asList(dummyStrs), root);
	    viewr.open();

		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File(oFile), root);

		return oFile;
	}
	

	private ArrayList<ScannerToken> loadTokens(String tFile) {
		tknList = new ArrayList<ScannerToken>();
		
		ArrayList<String> tknStr = MiniCCUtil.readFile(tFile);
		
		for(String str: tknStr) {
			if(str.trim().length() <= 0) {
				continue;
			}
			
			ScannerToken st = new ScannerToken();
			//[@0,0:2='int',<'int'>,1:0]
			String[] segs;
			if(str.indexOf("<','>") > 0) {
				str = str.replace("','", "'DOT'");
				
				segs = str.split(",");
				segs[1] = "=','";
				segs[2] = "<','>";
				
			}else {
				segs = str.split(",");
			}
			st.id = Integer.parseInt(segs[0].substring(segs[0].indexOf("@") + 1));
			st.lexeme = segs[1].substring(segs[1].indexOf("=") + 1);
			st.type  = segs[2].substring(segs[2].indexOf("<") + 1, segs[2].length() - 1);
			String[] lc = segs[3].split(":");
			st.line = Integer.parseInt(lc[0]);
			st.column = Integer.parseInt(lc[1].replace("]", ""));
			
			tknList.add(st);
		}
		
		return tknList;
	}

	private ScannerToken getToken(int index){
		if (index < tknList.size()){
			return tknList.get(index);
		}
		return null;
	}

	public ScannerToken matchToken(String type) {
		if(tokenIndex < tknList.size()) {
			ScannerToken next = tknList.get(tokenIndex);
			if(!next.type.equals(type)) {
				System.out.println("[ERROR]Parser: unmatched token, expected = " + type + ", " 
						+ "input = " + next.type + ",location = " + next.line + ":" + next.column);
			}
			else {
				tokenIndex++;
			}
			return next;
		}else {
			return null;
		}
	}

	//identifier
	public ASTIdentifier identifier() {
		ASTIdentifier id = new ASTIdentifier();
		token = matchToken("Identifier");
		id.tokenId = token.id;
		id.value = token.lexeme.substring(1,token.lexeme.length()-1);
		return id;
	}

	//operator
	public ASTToken operator(String op) {
		ASTToken opt = new ASTToken();
		token = matchToken(op);
		opt.tokenId = token.id;
		opt.value = token.lexeme.substring(1,token.lexeme.length()-1);
		return opt;
	}

	//char
	public ASTCharConstant charc() {
		ASTCharConstant cc = new ASTCharConstant();
		token = matchToken("CharacterConstant");
		cc.tokenId = token.id;
		cc.value = token.lexeme.substring(1,token.lexeme.length()-1);
		return cc;
	}

	//float
	public ASTFloatConstant floatc() {
		ASTFloatConstant fc = new ASTFloatConstant();
		token = matchToken("FloatingConstant");
		fc.tokenId = token.id;
		fc.value = Double.parseDouble(token.lexeme.substring(1,token.lexeme.length()-1));
		return fc;
	}

	//int
	public ASTIntegerConstant intc() {
		ASTIntegerConstant ic = new ASTIntegerConstant();
		token = matchToken("IntegerConstant");
		ic.tokenId = token.id;
		ic.value = Integer.parseInt(token.lexeme.substring(1,token.lexeme.length()-1));
		return ic;
	}

	//str
	public ASTStringConstant strl() {
		ASTStringConstant sc = new ASTStringConstant();
		token = matchToken("StringLiteral");
		sc.tokenId = token.id;
		sc.value = token.lexeme.substring(1,token.lexeme.length()-1);
		return sc;
	}

	//PROGRAM --> FUNC_LIST
	public ASTNode program() {
		ASTCompilationUnit p = new ASTCompilationUnit();
		ArrayList<ASTNode> fl = funcList();
		if(fl != null) {
			//p.getSubNodes().add(fl);
			p.items.addAll(fl);
		}
		p.children.addAll(p.items);
		return p;
	}

	//FUNC_LIST --> FUNC FUNC_LIST | e
	public ArrayList<ASTNode> funcList() {
		ArrayList<ASTNode> fl = new ArrayList<ASTNode>();
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("EOF")) {
			return null;
		}else {
			ASTNode f = func();
			fl.add(f);
			ArrayList<ASTNode> fl2 = funcList();
			if(fl2 != null) {
				fl.addAll(fl2);
			}
			return fl;
		}
	}

	//FUNC --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
	public ASTNode func() {
		ASTFunctionDefine fdef = new ASTFunctionDefine();
		ASTToken s = type();
		fdef.specifiers.add(s);
		fdef.children.add(s);
		
		ASTFunctionDeclarator fdec = new ASTFunctionDeclarator();
		ASTVariableDeclarator vd = new ASTVariableDeclarator();
		ASTIdentifier id = identifier();
		vd.identifier = id;
		fdec.declarator = vd;
		vd.children.add(id);
		fdec.children.add(vd);
		
		token = matchToken("'('");
		//todo:

		fdec.params = new ArrayList<>();
		List<ASTParamsDeclarator> apd = arguments();
		if(apd != null)
			fdec.params = apd;

		token = matchToken("')'");
		//todo:

		fdef.declarator = fdec;
		fdef.children.add(fdec);

		ASTCompoundStatement cs = codeBlock();
		fdef.body = cs;
		fdef.children.add(cs);

		return fdef;
	}

	//TYPE --> CHAR | DOUBLE | FLOAT | INT | VOID
	public String[] typeList = {"'char'", "'double'","'float'","'int'","'void'"};
	public ASTToken type() {
		ScannerToken st = tknList.get(tokenIndex);
		
		ASTToken t = new ASTToken();
		if(Arrays.asList(typeList).contains(st.type)) {
			t.tokenId = tokenIndex;
			t.value = st.lexeme;
			t.value = t.value.substring(1, t.value.length()-1);
			tokenIndex++;
		}
		//todo:
		return t;
	}

	//ARGUMENTS --> e | ARG_LIST
	public ArrayList<ASTParamsDeclarator> arguments() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("')'")) { //ending
			return null;
		}
		else {
			ArrayList<ASTParamsDeclarator> al = argList();
			return al;
		}
	}

	//ARG_LIST --> ARGUMENT ',' ARG_LIST | ARGUMENT
	public ArrayList<ASTParamsDeclarator> argList() {
		ArrayList<ASTParamsDeclarator> pdl = new ArrayList<ASTParamsDeclarator>();
		ASTParamsDeclarator pd = argument();
		pdl.add(pd);


		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("','")) {
			token = matchToken("','");
			//todo
			ArrayList<ASTParamsDeclarator> pdl2 = argList();
			pdl.addAll(pdl2);
		}
		
		return pdl;
	}
		
	//ARGUMENT --> TYPE ID
	public ASTParamsDeclarator argument() {
		ASTParamsDeclarator pd = new ASTParamsDeclarator();
		ASTToken t = type();
		pd.specfiers.add(t);
		pd.children.add(t);

		ASTVariableDeclarator vd =  new ASTVariableDeclarator();
		ASTIdentifier id = identifier();
		vd.identifier = id;
		vd.children.add(id);
		pd.declarator = vd;
		pd.children.add(vd);
		
		return pd;
	}

	//CODE_BLOCK --> '{' STMTS '}'
	public ASTCompoundStatement codeBlock() {
		token = matchToken("'{'");
		ASTCompoundStatement cs = new ASTCompoundStatement();

		ArrayList<ASTStatement> as = stmts(cs);
		if(as != null) {
			cs.blockItems.addAll(as);
		}

		token = matchToken("'}'");

		return cs;
	}

	//STMTS --> STMT STMTS | e
	public ArrayList<ASTStatement> stmts(ASTStatement cs) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'}'"))
			return null;
		else {
			ArrayList<ASTStatement> as = new ArrayList<ASTStatement>();
			ASTStatement s = stmt();
			cs.children.add(s);
			as.add(s);

			ArrayList<ASTStatement> as2 = stmts(cs);
			if(as2 != null)
				as.addAll(as2);
			return as;
		}
	}

	//STMT --> CODE_BLOCK | EXPR_STMT | SELC_STMT | DECL_STMT | ITER_STMT | RET_STMT | BREAK_STMT
	public ASTStatement stmt() {
		nextToken = tknList.get(tokenIndex);
		switch (nextToken.type) {
			case "'{'":
				return codeBlock();
			case "Identifier":
				return exprStmt();
			case "'if'":
				return selcStmt();
			case "'for'":
				return iterStmt();
			case "'return'":
				return returnStmt();
			case "'break'":
				return breakStmt();
			default:
				return declStmt();
		}
	}

	//EXPR_STMT --> EXPR ';'
	public ASTExpressionStatement exprStmt() {
		ASTExpressionStatement es = new ASTExpressionStatement();
		ASTExpression e = expr();
		ArrayList<ASTExpression> le = new ArrayList<>();
		le.add(e);
		es.exprs = le;
		es.children.add(e);

		token = matchToken("';'");

		return es;
	}


	//EXPR  EXPR_AND EXPR’
	public ASTExpression expr() {
		ASTExpression e = exprAnd();
		ASTBinaryExpression be = expr2(e);

		if (be != null) {
			return be;
		}else {
			return e;
		}
	}

	//EXPR’   (‘=’ | ‘+=’ | ‘-=’ | ‘*=’) EXPR_AND EXPR’ | ε
	public ASTBinaryExpression expr2(ASTExpression e) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'='") || nextToken.type.equals("'+='") || nextToken.type.equals("'-='") || nextToken.type.equals("'*='")) {
			ASTToken op = operator(nextToken.type);
			ASTBinaryExpression be = new ASTBinaryExpression();
			be.expr1 = e;
			be.children.add(e);
			be.op = op;
			be.children.add(op);
			be.expr2 = exprAnd();
			be.children.add(be.expr2);

			ASTBinaryExpression be2 = expr2(be);
			if(be2 != null) {
				return be2;
			}
			return be;
		}else {
			return null;
		}
	}

	//EXPR_AND  EXPR_RELA EXPR_AND’
	public ASTExpression exprAnd() {
		ASTExpression e = exprRela();
		ASTBinaryExpression be = exprAnd2(e);

		if (be != null) {
			return be;
		}else {
			return e;
		}
	}

	//EXPR_AND’  ‘&&’ EXPR_RELA EXPR_AND’ | ε
	public ASTBinaryExpression exprAnd2(ASTExpression e) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'&&'") ) {
			ASTToken op = operator(nextToken.type);
			ASTBinaryExpression be = new ASTBinaryExpression();
			be.expr1 = e;
			be.children.add(e);
			be.op = op;
			be.children.add(op);
			be.expr2 = exprRela();
			be.children.add(be.expr2);

			ASTBinaryExpression be2 = exprAnd2(be);
			if(be2 != null) {
				return be2;
			}
			return be;
		}else {
			return null;
		}
	}

	//EXPR_RELA  EXPR_ADD EXPR_RELA’
	public ASTExpression exprRela() {
		ASTExpression e = exprAdd();
		ASTBinaryExpression be = exprRela2(e);

		if (be != null) {
			return be;
		}else {
			return e;
		}
	}

	//EXPR_RELA’  (‘>’ | ‘<’ | ‘>=’ | ‘<=’) EXPR_ADD EXPR_RELA’ | ε
	public ASTBinaryExpression exprRela2(ASTExpression e) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'>'") || nextToken.type.equals("'<'") || nextToken.type.equals("'>='") || nextToken.type.equals("'<='")) {
			ASTToken op = operator(nextToken.type);
			ASTBinaryExpression be = new ASTBinaryExpression();
			be.expr1 = e;
			be.children.add(e);
			be.op = op;
			be.children.add(op);
			be.expr2 = exprAdd();
			be.children.add(be.expr2);

			ASTBinaryExpression be2 = exprRela2(be);
			if(be2 != null) {
				return be2;
			}
			return be;
		}else {
			return null;
		}
	}

	//EXPR_ADD  EXPR_MUL EXPR_ADD’
	public ASTExpression exprAdd() {
		ASTExpression e = exprMul();
		ASTBinaryExpression be = exprAdd2(e);

		if (be != null) {
			return be;
		}else {
			return e;
		}
	}

	//EXPR_ADD’  (‘+’ | ‘-’) EXPR_MUL EXPR_ADD’ | ε
	public ASTBinaryExpression exprAdd2(ASTExpression e) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'+'") || nextToken.type.equals("'-'")) {
			ASTToken op = operator(nextToken.type);
			ASTBinaryExpression be = new ASTBinaryExpression();
			be.expr1 = e;
			be.children.add(e);
			be.op = op;
			be.children.add(op);
			be.expr2 = exprMul();
			be.children.add(be.expr2);

			ASTBinaryExpression be2 = exprAdd2(be);
			if(be2 != null) {
				return be2;
			}
			return be;
		}else {
			return null;
		}
	}

	//EXPR_MUL  EXPR_POST EXPR_MUL’
	public ASTExpression exprMul() {
		ASTExpression e = exprPost();
		ASTBinaryExpression be = exprMul2(e);

		if (be != null) {
			return be;
		}else {
			return e;
		}
	}

	//EXPR_MUL’  (‘*’ | ‘/’ | ‘%’) EXPR_POST EXPR_MUL’ | ε
	public ASTBinaryExpression exprMul2(ASTExpression e) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'*'") || nextToken.type.equals("'/'") || nextToken.type.equals("'%'")) {
			ASTToken op = operator(nextToken.type);
			ASTBinaryExpression be = new ASTBinaryExpression();
			be.expr1 = e;
			be.children.add(e);
			be.op = op;
			be.children.add(op);
			be.expr2 = exprPost();
			be.children.add(be.expr2);

			ASTBinaryExpression be2 = exprMul2(be);
			if(be2 != null) {
				return be2;
			}
			return be;
		}else {
			return null;
		}
	}

	//EXPR_POST  EXPR_PRIM EXPR_POST’
	public ASTExpression exprPost() {
		ASTExpression e = exprPrim();
		ASTExpression be = exprPost2(e);

		if (be != null) {
			return be;
		}else {
			return e;
		}
	}

	//EXPR_POST’  (‘++’ | ‘--’) EXPR_POST’ | ‘[’ EXPR ‘]’ EXPR_POST’ | ε
	public ASTExpression exprPost2(ASTExpression e) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'++'") || nextToken.type.equals("'--'") ) {
			ASTToken op = operator(nextToken.type);
			ASTPostfixExpression be = new ASTPostfixExpression();
			be.expr = e;
			be.children.add(e);
			be.op = op;
			be.children.add(op);

			ASTExpression be2 = exprPost2(be);
			if(be2 != null) {
				return be2;
			}else {
				return be;
			}
		}else if (nextToken.type.equals("'['")){

			ASTArrayAccess aa = new ASTArrayAccess();
			aa.arrayName = e;
			aa.children.add(e);

			token = matchToken("'['");
			ASTExpression e1 = expr();
			aa.elements = new ArrayList<>();
			aa.elements.add(e1);
			aa.children.add(e1);

			token = matchToken("']'");

			ASTExpression be2 = exprPost2(aa);
			if(be2 != null) {
				return be2;
			}
			return aa;
		}else {
			return null;
		}
	}

	//EXPR_PRIM  ID | CONST_INT | CONST_FLOAT | CONST_CHAR | STR | ‘(’ EXPR ‘)’ | FUNC_CALL
	public ASTExpression exprPrim() {
		nextToken = tknList.get(tokenIndex);
		switch (nextToken.type) {
			case "Identifier": {
				ASTIdentifier id = identifier();
				nextToken = tknList.get(tokenIndex);
				if (nextToken.type.equals("'('")) {
					return funcCall(id);
				}else {
					return id;
				}
			}
			case "IntegerConstant":
				return intc();
			case "FloatingConstant":
				return floatc();
			case "CharacterConstant":
				return charc();
			case "StringLiteral":
				return strl();
			case "'('": {
				ASTExpression e = expr();
				token = matchToken("')'");
				return e;
			}
			default:
				return null;
		}
	}

	//FUNC_CALL  ID ‘(’ PARA_LIST ‘)’
	public ASTFunctionCall funcCall(ASTIdentifier id ) {
		ASTFunctionCall fc = new ASTFunctionCall();
		fc.funcname = id;
		fc.children.add(id);
		token = matchToken("'('");
		fc.argList = new ArrayList<>();
		paraList(fc);
		token = matchToken("')'");
		return fc;
	}

	//PARA_LIST  EXPR ‘,’ PARA_LIST | EXPR | ε
	public void paraList(ASTFunctionCall fc) {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("')'")) {
			return ;
		} else {
//			ArrayList<ASTExpression> al = new ArrayList<ASTExpression>();
			ASTExpression e = expr();
			fc.argList.add(e);
			fc.children.add(e);

			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals(",")) {
				token = matchToken("','");
				paraList(fc);
			} else {
				return ;
			}
		}
	}

	//DECL_STMT  TYPE INIT_LIST ‘;’
	public ASTDeclaration declStmt(){
		ASTDeclaration d = new ASTDeclaration();
		ASTToken t = type();
		d.specifiers = new ArrayList<>();
		d.specifiers.add(t);
		d.children.add(t);

//		ArrayList<ASTInitList> ils = initList();
//		d.initLists = ils;
		initList(d);

		token = matchToken("';'");
		return d;
	}

	//INIT_LIST  INIT_ITEM INIT_LIST’
	public void initList(ASTDeclaration d) {
		ASTInitList il = initItem();
		d.initLists = new ArrayList<>();
		d.initLists.add(il);
		d.children.add(il);

		initList2(d);
	}

	//INIT_LIST’  ‘,’ INIT_ITEM INIT_LIST’ | ε
	public void initList2(ASTDeclaration d) {
		nextToken =tknList.get(tokenIndex);
		if (nextToken.type.equals("';'")) {
			return;
		} else {
			token = matchToken("','");
			ASTInitList il = initItem();
			d.initLists.add(il);
			d.children.add(il);
			initList2(d);
		}
	}

	//INIT_ITEM  DECLR ‘=’ EXPR | DECLR
	public ASTInitList initItem() {
		ASTInitList il = new ASTInitList();
		ASTDeclarator d = declr();
		il.declarator = d;
		il.children.add(d);
		il.exprs = new ArrayList<>();
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'='")) {
			token = matchToken("'='");
			ASTExpression e = expr();
			il.exprs.add(e);
			il.children.add(e);
		}
		return il;
	}

	//DECLR  VAR | ARR
	//VAR ID
	public ASTDeclarator declr() {
		ASTVariableDeclarator vd = new ASTVariableDeclarator();
		ASTIdentifier id = identifier();
		vd.identifier = id;
		vd.children.add(id);

		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'['")) {
			return arr(vd);
		} else {
			return vd;
		}
	}

	//ARR  DECLR ‘[’ EXPR ‘]’
	public ASTArrayDeclarator arr(ASTDeclarator d) {
		ASTArrayDeclarator ad = new ASTArrayDeclarator();
		ad.declarator = d;
		ad.children.add(d);

		token = matchToken("'['");

		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("']'")){
			ad.expr = null;
		} else {
			ASTExpression e = expr();
			ad.expr = e;
			ad.children.add(e);
		}

		token = matchToken("']'");

		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("[")) {
			return arr(ad);
		} else {
			return ad;
		}
	}

	//SELC_STMT  ‘if’ ‘(’ EXPR ‘)’ STMT | ‘if’ ‘(’ EXPR ‘)’ STMT ‘else’ STMT
	public ASTSelectionStatement selcStmt() {
		ASTSelectionStatement ss = new ASTSelectionStatement();
		token = matchToken("'if'");
		token = matchToken("'('");

		ASTExpression e = expr();


		ss.cond = new LinkedList<>();
		ss.cond.add(e);
		ss.children.add(e);

		token = matchToken("')'");

		ASTStatement then = stmt();
		ss.then = then;

		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'else'")) {
			token = matchToken("'else'");
			ASTStatement otherwise = stmt();
			ss.otherwise = otherwise;
			ss.children.add(otherwise);

		} else {
			ss.otherwise = null;
		}
		return ss;
	}

	//ITER_STMT  ITER_DECL_STMT | ITER_EXPR_STMT;
	//ITER_EXPR_STMT  ‘for’ ‘(’ EXPR ‘;’ EXPR ‘;’ EXPR ’)’ STMT
	//ITER_DECL_STMT  ‘for’ ‘(’ DECLN ‘;’ EXPR ‘;’ EXPR ’)’ STMT
	public ASTStatement iterStmt() {
		token = matchToken("'for'");
		token = matchToken("'('");
		nextToken = tknList.get(tokenIndex);
		if(Arrays.asList(typeList).contains(nextToken.type)) {
			ASTIterationDeclaredStatement ids = new ASTIterationDeclaredStatement();
			ASTDeclaration d = declStmt();
			ids.init = d;
			ids.children.add(d);

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals(";")) {
				ids.cond = null;
			} else {
				ASTExpression e2 = expr();
				ids.cond = new LinkedList<>();
				ids.cond.add(e2);
				ids.children.add(e2);
			}
			token = matchToken("';'");

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals(";")) {
				ids.step = null;
			} else {
				ASTExpression e3 = expr();
				ids.step = new LinkedList<>();
				ids.step.add(e3);
				ids.children.add(e3);
			}
			token = matchToken("')'");

			ASTStatement stat = stmt();
			ids.stat = stat;
			ids.children.add(stat);
			return ids;
		}
		else {
			ASTIterationStatement is = new ASTIterationStatement();
			if (nextToken.type.equals("';'")) {
				is.init = null;
			} else {
				ASTExpression e1 = expr();
				is.init = new LinkedList<>();
				is.init.add(e1);
				is.children.add(e1);
			}
			token = matchToken("';'");

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals(";")) {
				is.cond = null;
			} else {
				ASTExpression e2 = expr();
				is.cond = new LinkedList<>();
				is.cond.add(e2);
				is.children.add(e2);
			}
			token = matchToken("';'");

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals(";")) {
				is.step = null;
			} else {
				ASTExpression e3 = expr();
				is.step = new LinkedList<>();
				is.step.add(e3);
				is.children.add(e3);
			}
			token = matchToken("')'");

			ASTStatement stat = stmt();
			is.stat = stat;
			is.children.add(stat);
			return is;
		}

	}

	//RETURN_STMT --> 'return' EXPR ';' | 'return' ';'
	public ASTReturnStatement returnStmt() {
		ASTReturnStatement rs = new ASTReturnStatement();
		token = matchToken("'return'");

		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("';'")) {
			rs.expr = null;
		} else {
			rs.expr.add(expr());
		}

		token = matchToken("';'");

		return rs;
	}

	//BREAK_STMT --> 'break' ';'
	public ASTBreakStatement breakStmt() {
		token = matchToken("'break'");
		ASTBreakStatement bs = new ASTBreakStatement();

		token = matchToken("';'");

		return bs;
	}
}
