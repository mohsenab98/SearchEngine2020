package Classes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
// https://stackoverflow.com/questions/22043451/how-to-convert-integers-to-base64-0-9a-za-z
public class AlphabetEncoder
{
    private static final char[] ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
    private static final int ENCODE_LENGTH = ALPHABET.length;

    public String encode(int victim){
        List<Character> list = new ArrayList<>();

        do {
            list.add(ALPHABET[victim % ENCODE_LENGTH]);
            victim /= ENCODE_LENGTH;
        } while (victim > 0);

        Collections.reverse(list);
        String ans = "";
        for(Character ch : list){
            ans += ch;
        }
        return ans;
    }

    public int decode (String encoded){
        int ret = 0;
        char c;
        for (int index = 0; index < encoded.length(); index++) {
            c = encoded.charAt(index);
            ret *= ENCODE_LENGTH;
            ret += Arrays.binarySearch(ALPHABET, c);
        }
        return ret;
    }
}
