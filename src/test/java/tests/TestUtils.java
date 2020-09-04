package tests;

import org.junit.Ignore;
import org.junit.Test;
import utils.OTMUtils;

public class TestUtils {

    @Ignore
    @Test
    public void test_read_int_table() {
        String str = "1,2,3;4,5,6;7,8,9";
        int [][] table = OTMUtils.read_int_table(str);
        for(int i=0;i<3;i++)
            for(int j=0;j<3;j++)
                System.out.println(table[i][j]);
    }

    @Ignore
    @Test
    public void test_write_int_table() {
        int [][] table = new int[3][2];
        for(int i=0;i<table.length;i++)
            for(int j=0;j<table[i].length;j++)
                table[i][j] = i+j;
        System.out.println( OTMUtils.write_int_table(table));
    }

}
