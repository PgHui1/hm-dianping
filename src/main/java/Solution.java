import java.util.ArrayList;
import java.util.List;

public class Solution {
    List<List<Integer>> result = new ArrayList<>();
    public List<List<Integer>> combine(int n, int k) {
        backtracking(n,k,1,new ArrayList<>());
        return result;
    }

    public void backtracking(int n, int k,int startIndex,List<Integer> list) {
        if(list.size() == k){
            result.add(new ArrayList<>(list));
            return;
        }
        for(int i =startIndex;i <= n;i ++){
            list.add(i);
            backtracking(n,k,i+1,list);
            list.remove(list.size()-1);
        }
    }


    public static void main(String[] args) {
        int[][] arr = new int[][]{
                {4,3,3,6,6,3,2,1,0,7},
                {1,8,2,8,5,9,2,8,3,1},
                {8,0,9,2,4,3,2,4,3,7},
                {1,2,2,6,3,0,3,9,7,0},
                {7,4,3,8,8,3,2,4,6,8},
                {2,8,9,2,9,3,0,8,7,8},
                {8,9,9,4,6,3,3,4,9,6},
                {2,8,3,8,1,3,7,3,0,7},
                {2,1,1,6,4,1,0,8,1,6},
                {4,1,3,6,3,4,4,4,0,3}
        };
        Solution solution = new Solution();

    }

}
