import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import net.sf.jsqlparser.parser.CCJSqlParserManager;


//class FilterExpressionVisitorAdapter extends ExpressionVisitorAdapter{
//    int depth = 0;
//    public void processLogicalExpression( BinaryExpression expr, String logic){
//        System.out.println(StringUtils.repeat("-", depth) + logic);
//
//        depth++;
//        expr.getLeftExpression().accept(this);
//        expr.getRightExpression().accept(this);
//        if(  depth != 0 ){
//            depth--;
//        }
//    }
//
//    @Override
//    protected void visitBinaryExpression(BinaryExpression expr) {
//        if (expr instanceof ComparisonOperator) {
//            System.out.println(StringUtils.repeat("-", depth) +
//                    "left=" + expr.getLeftExpression() +
//                    "  op=" +  expr.getStringExpression() +
//                    "  right=" + expr.getRightExpression() );
//        }
//        super.visitBinaryExpression(expr);
//    }
//
//    @Override
//    public void visit(AndExpression expr) {
//        processLogicalExpression(expr, "AND");
//
//    }
//    @Override
//    public void visit(OrExpression expr) {
//        processLogicalExpression(expr, "OR");
//    }
//    @Override
//    public void visit(Parenthesis parenthesis) {
//        parenthesis.getExpression().accept(this);
//    }
//
//}
//

public class Parsing {

//    public static void parseWhereClauseToFilter(String whereClause ){

//        try {
//            Expression expr = CCJSqlParserUtil.parseCondExpression(whereClause);
//            FilterExpressionVisitorAdapter adapter = new FilterExpressionVisitorAdapter();
//            expr.accept(adapter);
//
//        } catch (JSQLParserException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//    }

    public static void main(String[] args) throws IOException, JSQLParserException {
//        CCJSqlParserManager pm = new CCJSqlParserManager();
        QueryVectorGeneration qvg = new QueryVectorGeneration();
        qvg.featurecreator();
        qvg.getQueryVector();
        File folder = new File("C:\\Users\\Pawan Joshi\\tpch-dbgen\\queries\\Generated_Queries");
        File[] listOfFiles = folder.listFiles();

        Hashtable<String, JSONObject> Dict = new Hashtable<>();
        JSONArray JSONMain = new JSONArray();
        FileWriter fw = new FileWriter("testout.json");

        //atleast one file in the folder
        if (listOfFiles.length > 0) {


            for (File file : listOfFiles) {
                JSONObject Predicates = new JSONObject();

                System.out.println(file);
                BufferedReader br = new BufferedReader(new FileReader(file));    //we want to access the content line wise
                StringBuilder mainstr = new StringBuilder();
                String st;
                int cnt = 0;  //flag to check for the first where keyword
                if (Files.readAllLines(Paths.get(String.valueOf(file))).contains("lineitem"))  // if line item present

                {
                    while ((st = br.readLine()) != null)
                        if (cnt == 0) {
                            if (st.startsWith("where")) {
//                                mainstr.append(st);   //JSQLParser does not need the keyword "where" while it parses for the predicates
                                cnt += 1;
                            }
                        } else if (!st.startsWith("go") && !st.startsWith("group") && !st.startsWith("order") && !st.startsWith("set")) {
                            mainstr.append(st).append(" ");
                        }
                }

                System.out.println(mainstr.toString());
                if (mainstr.toString().isEmpty()) {
                    continue;
                }
//                parseWhereClauseToFilter(mainstr.toString());
                try {
                    Expression expr;

                    expr = CCJSqlParserUtil.parseCondExpression(mainstr.toString(), true);
//                JSONObject jsoncontent = (JSONObject) getJSONContent(expr, Predicates);
                    if (expr instanceof AndExpression) {              //check if the expression is of type AND
                        JSONObject interobj = new JSONObject();
                        interobj = (JSONObject) getANDPredicates(expr);
                        if (interobj.length() > 0) {
                            Predicates.put("QueryNumber", file.getName());    //puts the query number as file name
                            Predicates.put("ANDPredicates", interobj);  //gets the content inside the and block

                        }

                    }
                    if (expr instanceof OrExpression) {                 //checks if expression of type OR
                        JSONObject interobj = new JSONObject();
                        interobj = (JSONObject) getORPredicates(expr);
                        if (interobj.length() > 0) {
                            Predicates.put("QueryNumber", file.getName());
                            Predicates.put("ORPredicates", interobj); //gets the content inside the or block

                        }

                    }

                    if (Predicates.length() > 0) {                //append each parsed files predicate along with the filename
                        Dict.put(file.getName(), Predicates);
                        JSONMain.put(Predicates);
                        System.out.println(Predicates);
                    }
                } catch (JSQLParserException | JSONException ignored) {

                }


            }
            Files.write(Paths.get("test.json"), (JSONMain).toString().getBytes());     //json format output
            try (Writer writer = new FileWriter("MainCsvFile.csv")) {               //csv format output
                writer.append("id").append(',').append("Filename").append(',').append("Predicates").append("\n");
                int i = 1;
                for (Map.Entry<String, JSONObject> entry : Dict.entrySet()) {
                    String val = entry.getValue().toString();
                    writer.append("" + i).append(',').append(entry.getKey())
                            .append(',')
                            .append(entry.getValue().toString())
                            .append("\n");
                    i++;
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }

    }

    /**
     * Handles the AND block
     * @param expr
     * @return JSON Object of everything inside AND block
     * @throws JSONException
     */
    private static Object getANDPredicates(Expression expr) throws JSONException {
        JSONObject ANDPredicate = new JSONObject();
        JSONArray SimplePredicate = new JSONArray();

        JSONArray NestedOR = new JSONArray();
        {

            expr.accept(new ExpressionVisitorAdapter() {


                @Override
                protected void visitBinaryExpression(BinaryExpression expr) {

                    if (expr instanceof ComparisonOperator) {
                        if ((expr.getLeftExpression().toString().startsWith("l_") && !expr.getRightExpression().toString().contains("_")) && !(expr.getRightExpression().toString().startsWith("(SELECT")))
                        {  JSONObject PredicateObject=new JSONObject();
                            try {
                                PredicateObject.put("ColumnName",expr.getLeftExpression());
                                PredicateObject.put("Operator",expr.getStringExpression());
                                PredicateObject.put("Value",expr.getRightExpression());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            SimplePredicate.put(PredicateObject);

//                            SimplePredicate.put("ColumnName:" + expr.getLeftExpression() + "  Operator:" + expr.getStringExpression() + "  Value:" + expr.getRightExpression());
                            System.out.println("ColumnName:" + expr.getLeftExpression() + "  Operator:" + expr.getStringExpression() + "  Value:" + expr.getRightExpression());
                        }

                    }


                    if(expr.getRightExpression() instanceof InExpression) {   //for handling of IN operator
                        {
                            if (((InExpression) expr.getRightExpression()).getLeftExpression().toString().startsWith("l_")) {
                                List<String> list = Collections.singletonList(((InExpression) expr.getRightExpression()).getRightItemsList().toString());
                                String val=" ";
                                for (Object l : list.toString().split(",")) {
                                    Pattern p = Pattern.compile("\'([^\"]*)\'");
                                    Matcher m = p.matcher(l.toString());
                                    while (m.find()) {
                                       val= (m.group(1));
                                    }
                                    JSONObject PredicateObject=new JSONObject();
                                    try {
                                        PredicateObject.put("ColumnName",((InExpression) expr.getRightExpression()).getLeftExpression());
                                        PredicateObject.put("Operator","=");
                                        PredicateObject.put("Value",val);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    NestedOR.put(PredicateObject);
//                                    NestedOR.put("ColumnName:" + ((InExpression) expr.getRightExpression()).getLeftExpression() + "  Operator:=" + "  Value=" + l);
                                    System.out.println("ColumnName:" + expr.getLeftExpression() + "  Operator" + expr.getStringExpression() + "  Value:" + expr.getRightExpression());


                                }

                            }
                        }

                    }
                    if(expr instanceof OrExpression){
                        try {
                            NestedOR.put(getORPredicates(expr));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


                    if(!(expr instanceof OrExpression)) {
                        super.visitBinaryExpression(expr);
                    }
                }
            });
            if(SimplePredicate.length()>0){
                ANDPredicate.put("SimplePredicates", SimplePredicate);
            }
            if(NestedOR.length()>0){
                ANDPredicate.put("NestedOR",NestedOR);
            }



        }
        return ANDPredicate;
    }

    private static Object getORPredicates(Expression expr) throws JSONException {


        JSONArray SimplePredicate = new JSONArray();
        JSONObject ORPredicate = new JSONObject();
        JSONArray NestedAnd = new JSONArray();
        {

            expr.accept(new ExpressionVisitorAdapter() {

                @Override
                protected void visitBinaryExpression(BinaryExpression expr) {

                    if (expr instanceof ComparisonOperator) {         //only need right expression for the value tag in json file
                        if ((expr.getLeftExpression().toString().startsWith("l_")&& !expr.getRightExpression().toString().contains("_"))&& !(expr.getRightExpression().toString().startsWith("(SELECT")))
                        {
                            JSONObject PredicateObject=new JSONObject();
                            try {
                                PredicateObject.put("ColumnName",expr.getLeftExpression());
                                PredicateObject.put("Operator",expr.getStringExpression());
                                PredicateObject.put("Value",expr.getRightExpression());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            SimplePredicate.put(PredicateObject);

//                            SimplePredicate.put("ColumnName:" + expr.getLeftExpression() + "  Operator:" + expr.getStringExpression() + "  Value:" + expr.getRightExpression());
                            System.out.println("ColumnName:" + expr.getLeftExpression() + "  Operator:" + expr.getStringExpression() + "  Value:" + expr.getRightExpression());
                        }

                    }
                    if(expr instanceof AndExpression){
                        try {
                            NestedAnd.put(getANDPredicates(expr));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if(!(expr instanceof AndExpression)) {
                        super.visitBinaryExpression(expr);
                    }
                }
            });
            if(NestedAnd.length()>0){
                ORPredicate.put("NestedAND",NestedAnd);
            }
            if(SimplePredicate.length()>0){
                ORPredicate.put("SimplePredicates", SimplePredicate);
            }


        }
        return ORPredicate;
    }
}







