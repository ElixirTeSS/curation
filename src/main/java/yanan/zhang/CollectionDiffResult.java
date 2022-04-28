package yanan.zhang;

import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Diff result set
 * @author Yanan Zhang
 **/
@ToString
public class CollectionDiffResult<T> implements Serializable {

    private static final long serialVersionUID = -6083068195893077830L;

    private List<T> insertList;

    private List<T> updateList;

    private List<T> deleteList;

    public List<T> getInsertList() {
        return insertList;
    }

    public void setInsertList(List<T> insertList) {
        this.insertList = insertList;
    }

    public List<T> getUpdateList() {
        return updateList;
    }

    public void setUpdateList(List<T> updateList) {
        this.updateList = updateList;
    }

    public List<T> getDeleteList() {
        return deleteList;
    }

    public void setDeleteList(List<T> deleteList) {
        this.deleteList = deleteList;
    }

    public List<T> getChangedList() {
        List<T> changedList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(insertList)) {
            changedList.addAll(insertList);
        }
        if(CollectionUtils.isNotEmpty(updateList)) {
            changedList.addAll(updateList);
        }
        if(CollectionUtils.isNotEmpty(deleteList)) {
            changedList.addAll(deleteList);
        }
        return changedList;
    }

    public void addInsert(T bean) {
        if(insertList == null) {
            insertList = new ArrayList<>();
        }
        insertList.add(bean);
    }

    public void addUpdate(T bean) {
        if(updateList == null) {
            updateList = new ArrayList<>();
        }
        updateList.add(bean);
    }

    public void addDelete(T bean) {
        if(deleteList == null) {
            deleteList = new ArrayList<>();
        }
        deleteList.add(bean);
    }

}
