---
description: Remove blank lines inside Java method bodies to make code more compact.
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Overview

This command removes blank lines inside Java method bodies using AST parsing for accuracy. It preserves:
- Blank lines between methods
- Blank lines between class members (fields, static blocks, etc.)
- Blank lines between import statements
- Blank lines after class/interface/enum declarations
- License header formatting

## Prerequisites

This skill requires the `javalang` Python library for Java AST parsing. Install it first:

```bash
pip3 install --break-system-packages javalang
```

## Execution Steps

1. **Determine target scope** based on user input:
   - If `$ARGUMENTS` contains a file path: Process only that file
   - If `$ARGUMENTS` contains a directory path: Process all `.java` files in that directory (recursively)
   - If `$ARGUMENTS` is empty: Process all `.java` files in the current project's `src` directories

2. **Create a Python script** using `javalang` for AST-based blank line removal:

```python
#!/usr/bin/env python3
"""
Remove blank lines inside Java method bodies using AST parsing.
This ensures only lines inside actual method bodies are affected,
not blank lines between class members, fields, or other declarations.
"""

import sys
import javalang

def get_method_positions(java_code):
    """
    Parse Java code and return positions of method bodies.
    Returns list of (start_line, end_line) tuples for each method body.
    """
    try:
        tree = javalang.parse.parse(java_code)
    except Exception as e:
        return []

    lines = java_code.split('\n')
    method_positions = []

    def process_type(type_decl):
        """Process a class/interface/enum declaration."""
        # Process methods
        if hasattr(type_decl, 'methods') and type_decl.methods:
            for method in type_decl.methods:
                pos = get_node_body_position(method, lines)
                if pos:
                    method_positions.append(pos)

        # Process constructors
        if hasattr(type_decl, 'constructors') and type_decl.constructors:
            for constructor in type_decl.constructors:
                pos = get_node_body_position(constructor, lines)
                if pos:
                    method_positions.append(pos)

        # Process nested types
        if hasattr(type_decl, 'body') and type_decl.body:
            for member in type_decl.body:
                if isinstance(member, javalang.tree.ClassDeclaration):
                    process_type(member)
                elif isinstance(member, javalang.tree.InterfaceDeclaration):
                    process_type(member)
                elif isinstance(member, javalang.tree.EnumDeclaration):
                    process_type(member)

    # Process compilation unit types
    if tree.types:
        for type_decl in tree.types:
            process_type(type_decl)

    return method_positions

def get_node_body_position(node, lines):
    """
    Get the line range of a method/constructor body.
    Returns (start_line, end_line) or None if no body.
    """
    if not hasattr(node, 'position') or node.position is None:
        return None

    # Find the opening brace of the method body
    start_line = node.position[0] - 1  # Convert to 0-based

    # Look for opening brace from the method declaration
    brace_count = 0
    body_start = None
    body_end = None

    for i in range(start_line, len(lines)):
        line = lines[i]
        for char_idx, char in enumerate(line):
            if char == '{':
                if brace_count == 0:
                    body_start = i
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0 and body_start is not None:
                    body_end = i
                    return (body_start, body_end)

    return None

def remove_blank_lines_in_methods(content):
    """
    Remove blank lines only inside method bodies.
    Preserves all other blank lines (between methods, fields, etc.)
    """
    lines = content.split('\n')
    method_positions = get_method_positions(content)

    if not method_positions:
        return content

    # Create a set of line indices that are inside method bodies
    lines_in_methods = set()
    for start, end in method_positions:
        for i in range(start + 1, end):  # Exclude the braces themselves
            lines_in_methods.add(i)

    # Remove blank lines only if they are inside method bodies
    result = []
    for i, line in enumerate(lines):
        # Keep the line if it's not blank, or if it's not inside a method body
        if line.strip() != '' or i not in lines_in_methods:
            result.append(line)

    return '\n'.join(result)

def process_file(filepath):
    """Process a single Java file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        new_content = remove_blank_lines_in_methods(content)

        if content != new_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            return True
        return False
    except Exception as e:
        print(f"Error processing {filepath}: {e}", file=sys.stderr)
        return False

def main():
    if len(sys.argv) < 2:
        print("Usage: python remove_blank_lines_ast.py <file1.java> [file2.java ...]")
        sys.exit(1)

    processed = 0
    modified = 0
    for filepath in sys.argv[1:]:
        if process_file(filepath):
            modified += 1
        processed += 1

    print(f"Processed {processed} files, modified {modified} files")

if __name__ == "__main__":
    main()
```

3. **Execute the script** on the target files:

   For a single file:
   ```bash
   python3 /tmp/remove_blank_lines_ast.py <file_path>
   ```

   For all Java source files in a project:
   ```bash
   find <project_path> -name "*.java" -type f -path "*/src/*" ! -path "*/target/*" | xargs python3 /tmp/remove_blank_lines_ast.py
   ```

4. **Run spotless:apply** to restore any required blank lines (e.g., after anonymous class braces):
   ```bash
   ./mvnw spotless:apply
   ```

5. **Verify** with spotless and checkstyle:
   ```bash
   ./mvnw spotless:check checkstyle:check
   ```

6. **Clean up** the temporary script:
   ```bash
   rm /tmp/remove_blank_lines_ast.py
   ```

7. **Report results** to the user with:
   - Number of files processed
   - Number of files modified
   - Use `git diff --stat` to show changes summary

## Example

Before:
```java
public static Completer create(final MetaDataLoader loader) {
    List<Completer> completers = new ArrayList<>();

    // Keywords
    completers.add(new StringsCompleter(SQL_KEYWORDS));

    // Commands
    completers.add(new StringsCompleter(ISQL_COMMANDS));

    return new AggregateCompleter(completers);
}
```

After:
```java
public static Completer create(final MetaDataLoader loader) {
    List<Completer> completers = new ArrayList<>();
    // Keywords
    completers.add(new StringsCompleter(SQL_KEYWORDS));
    // Commands
    completers.add(new StringsCompleter(ISQL_COMMANDS));
    return new AggregateCompleter(completers);
}
```

## Notes

- This command modifies files in-place
- Uses `javalang` library for accurate AST-based Java parsing
- Always review changes with `git diff` before committing
- The script preserves blank lines between class members and methods
- Run `spotless:apply` after to ensure formatting compliance
