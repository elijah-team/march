import valuebased.classes.OpenValueBased;

class Main {
  final OpenValueBased vb = new OpenValueBased();
  {
    final OpenValueBased localVb = new OpenValueBased();
    final Object objectVb = new OpenValueBased();

    synchronized (<warning descr="Synchronization on instance of value-based class">vb</warning>) {}
    synchronized (<warning descr="Synchronization on instance of value-based class">localVb</warning>) {}
    synchronized (<warning descr="Synchronization on instance of value-based class">objectVb</warning>) {}
    synchronized (OpenValueBased.class) {}
    f(vb);
    g(vb);
  }

  void f(OpenValueBased vb) {
    synchronized (<warning descr="Synchronization on instance of value-based class">vb</warning>) {}
  }

  void g(Object vb) {
    synchronized (vb) {}
  }

  @SuppressWarnings("synchronization")
  void h(OpenValueBased vb) {
    synchronized (vb) {}
  }

  void i(Integer i) {
    synchronized (<warning descr="Synchronization on instance of value-based class">i</warning>) {}
  }
}
