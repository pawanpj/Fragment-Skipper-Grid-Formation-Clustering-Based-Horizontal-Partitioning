import com.sun.xml.internal.bind.v2.runtime.output.StAXExStreamWriterOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryVectorGeneration {
    public JSONArray featurecreator() {
        JSONParser jsonParser = new JSONParser();

        JSONArray featureList = null;
        try (FileReader reader = new FileReader("feature.json")) {
            //Read JSON file
            JSONObject obj = (JSONObject) jsonParser.parse(reader);

            featureList = (JSONArray) obj.get("Features");
            System.out.println(featureList);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return featureList;
    }
    public void getQueryVector(){
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("test.json"))
        {
            //Read JSON file
            JSONArray arr = (JSONArray) jsonParser.parse(reader);

            System.out.println(arr.get(89));
            JSONArray compr = new JSONArray();
            JSONObject genquery = (JSONObject) arr.get(89);
            JSONObject ORpred = (JSONObject) genquery.get("ORPredicates");
            JSONArray NestAND = (JSONArray) ORpred.get("NestedAND");
            for (Object simplePred: NestAND) {
                JSONObject newsimp= (JSONObject) simplePred;
                JSONArray simppredArr = (JSONArray) newsimp.get("SimplePredicates");
                for (Object pred: simppredArr)
                {
                  compr.add(pred);
                }
                JSONArray NestORArr = (JSONArray) newsimp.get("NestedOR");
                for(Object pred1: NestORArr)
                {
                    compr.add(pred1);
                }

            }
            System.out.println(compr);
            JSONArray features = featurecreator();
            Integer QueryVector[] = new Integer[0];
            List<Integer> Qv
                    = new ArrayList<Integer>(
                    Arrays.asList(QueryVector));

            for (Object obj: features) {
                boolean flag=false;
                JSONArray Arr = (JSONArray) obj;
                for (Object ob: Arr) {
                    for (Object check:compr) {
                        if (ob.equals(check)){
                            flag=true;
                            break;

                        }
                        else {
                            flag=false;

                        }
                    }



                }
                if(flag==true) {
                    Qv.add(0);
                }
                else{
                    Qv.add(1);
                }

            }
            System.out.println(Qv);






        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
