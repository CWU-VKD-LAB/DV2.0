## DV runtime bundle (`run/`)

### Why `DV.zip` may look “empty”

`DV.zip` is tracked with **Git LFS**. If you obtained this repository via GitHub’s “Download ZIP” button (or otherwise without Git LFS), you likely received a small *pointer* file instead of the real `DV.zip`.

Fix:

- Install Git LFS, then from the repo root run:
  - `git lfs install`
  - `git lfs pull`

After that, `run/DV.zip` should be hundreds of MB and will contain `DV.jar` and `run.bat`.

### Running on Windows

1. Install **Java 17**.
2. Extract `DV.zip` to a folder (for example `C:\DV`).
3. Launch using **`run.bat`** (recommended), or run:

```bat
java -jar DV.jar
```

### Rebuilding `DV.zip`

From the repo root: `scripts\package-dv.bat` (see root `README.md`).

