public class Parse {

    public String NumWithoutUnits(String term){
            int indexAfterDot;
            float numberInTerm = Float.parseFloat(term.replaceAll("[\\D]", ""));
            if(term.contains(".")){
                indexAfterDot = term.length() - (term.indexOf(".") + 1);
                numberInTerm = numberInTerm/(float)Math.pow(10, indexAfterDot);
            }

            int range = 0;
            if((numberInTerm >= 1000 && numberInTerm < 1000000)){
                range = 1;
            }else if((numberInTerm >= 1000000 && numberInTerm < 1000000000)){
                range = 2;
            }else if(numberInTerm >= 1000000000 ){
                range = 3;
            }else if(term.contains("Thousand")){
                range = 4;
            }else if( term.contains("Million")){
                range = 5;
            }else if(term.contains("Billion")){
                range = 6;
            }

            switch (range){
                case 1:
                    return String.format("%.03f",numberInTerm/1000) +"K";
                case 2:
                    return String.format("%.03f", numberInTerm/1000000) +"M";
                case 3:
                    return String.format("%.03f",numberInTerm/1000000000) +"B";
                case 4:
                    return numberInTerm +"K";
                case 5:
                    return numberInTerm +"M";
                case 6:
                    return numberInTerm +"B";
            }

            return term;
    }

    public String Price(String term){
        if(term.contains("$")){
            term = term.replace("$", "");
            term = term + " Dollars";
        }

        if(term.contains("U.S.")){
            term = term.replace("U.S.", "");
        }

        int indexAfterDot;
        float numberInTerm = Float.parseFloat(term.replaceAll("[\\D]", ""));
        if(term.contains(".")){
            String numberInTermStr = numberInTerm + "";
            indexAfterDot = numberInTermStr.length() - (numberInTermStr.indexOf(".") + 1);
            numberInTerm = numberInTerm/(float)Math.pow(10, indexAfterDot);
        }

        if(numberInTerm >= 1000000){
            return numberInTerm/1000000 + " M" + " Dollars";
        }

        if(term.contains("million") || term.contains("m ")){
            return numberInTerm + " M" + " Dollars";
        }

        if(term.contains("billion") || term.contains("bn ")){
            return numberInTerm * 1000 + " M" + " Dollars";
        }

        return term;
    }
}
