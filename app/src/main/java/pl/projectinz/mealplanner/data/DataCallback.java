package pl.projectinz.mealplanner.data;

// callback for Firebase calls (they are async, the result comes later)
// use Void if there is no result
public interface DataCallback<T> {

    void onSuccess(T result);

    void onError(Exception e);
}
