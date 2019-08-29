# booksFromWorldsave

Extract books from a world save ("Anvil" format).

## Usage

Replace `Civcraft 2.0` with the `item_origin` code of the world you're extracting books from.
Make sure to quote the space, like in the example below. This field will be stored with every book entry.

The program outputs the books to "standard out", as newline-separated JSON objects.
Make sure to redirect them to a file of your choice (here: `output.json`), so they don't spam the console.

    cd path/to/worldsave/world
    java -jar build/libs/booksFromWorldsave-all.jar "Civcraft 2.0" > output.json`

## Compilation

- Windows: `gradlew build`
- Mac/Linux: `./gradlew build`

