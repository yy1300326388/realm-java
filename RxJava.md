# RxJava POC

This documents contains thoughts and challenges on how to implement RxJava or other reactive frameworks


## API Design

There are two options:

- Baking in support for RxJava direct. Most likely by adding a .observable() on relevant objects that
  return an RxJava Observable for that type.

- Having RxJava as a addon that is optional, preferably in its own package. This will require factory
  constructors for all observable types ie. RxRealm.observable(RealmObject obj)


The former is a lot more fluent but we introduce a dependency on RxJava/RxAndroid which are still very
young projects.

The later doesn't read as nice, but reduces the coupling to be optional, and makes it possible to
not pollute the current API.

## Challenges

- Thread model is a challenge. We have no control over which thread observable items are read.
  Introducing thread restrictions will reduce the usefullness a lot.

- The proposed thread handover can possible do this for us?

- Perhaps restrict it to only ValueTypes?

- Calling Realm.close() will most likely get **very** confusing.

## Tread safety

- If you don't switch threads using RxJava, there should be no hindrance using it. However it is unclear
  how many methods except the below do switch threads behind the users back.

    Observable.subscribeOn(Schedulers.io())
    Subscriber.observeOn(AndroidSchedulers.mainThread())

Some discussions on RxJava and thread safety:

- http://stackoverflow.com/questions/18822230/safely-effectively-dispose-from-an-active-observable-once-my-observer-get-the-de/18823717?noredirect=1#answer-18823717
- file:///Users/cm/Downloads/Rx%20Design%20Guidelines.pdf


## Tests

Some usage examples can be found in RxJavaTest.java