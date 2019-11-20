public class main {
    //ddddddddddddddd evgenyyyyyy
    public static void main(String[] args){

    /////// ReadFile tests ///////
        String path = "Jackpot!";
        ReadFile rd = new ReadFile(path);
        System.out.println(rd.getPath());
        rd.setPath("C:\\Users\\EvgeniyU\\Desktop\\ThirdYear\\DataRetrieval\\corpusTest");
        System.out.println(rd.getPath());
        rd.printPath();
        rd.filesSeparator();

    /////// Parse tests ///////
        //Parse p = new Parse();
        //System.out.println(p.NumWithoutUnits("1010.56"));
        //System.out.println(p.NumWithPercent("15 percent"));
        //System.out.println(p.NumWithDates("15 May"));
        //System.out.println(p.Price("20.6m Dollars"));
    }
}
