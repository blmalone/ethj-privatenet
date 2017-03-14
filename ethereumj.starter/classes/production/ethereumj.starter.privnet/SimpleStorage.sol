contract SimpleStorage {
  int storedData;

  function set(int x) {
    storedData = x;
  }
  function get() returns (int) {
    return storedData;
  }
}