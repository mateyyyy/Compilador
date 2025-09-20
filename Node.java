public class Node {
    public String op;
    public Node left;
    public Node right;
    public String type;
    public String nodeType; //puede ser Operation, Leaf, Declaration, Funcion

    public Node(String op, Node left, Node right, String nodeType) {
        this.op = op;
        if(nodeType != "Leaf"){
            this.left = left;
            this.right = right;
        }
    }

    public String checkType(){
        if(this.nodeType == "Operation" || this.nodeType == "Declaration"){ //  
        String Rtype = right.checkType();
        if(Rtype == left.checkType()){
            this.type = Rtype;
            return Rtype;
        }
        return null;
        }
        else{
            return this.type;
        }
    }
}