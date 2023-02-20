package PoolGame.objects;

import java.util.Stack;

public class MementoCaretaker {
    private Stack<SystemMemento> mementoStack;

    public MementoCaretaker(){
        mementoStack = new Stack<>();
    }

    public void saveMemento(SystemMemento memento){
        mementoStack.push(memento);
    }

    public SystemMemento loadLastMemento(){
        return mementoStack.pop();
    }

    public boolean isEmpty(){
        if(mementoStack.isEmpty())
            return true;

        return false;
    }
}
