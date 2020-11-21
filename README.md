# lssl - Lisp Shading Language

lssl is a Lisp(y) graphics shader language that compiles to [SPIR-V](https://www.khronos.org/registry/spir-v/) assembly.
It is inspired by GLSL and Clojure (and also written in Clojure).

> :warning: **lssl is just a personal experiment at this stage and shold not be used for anything serious** :warning:
> currently it only compiles very basic programs

## Rationale

I'm not sure really at this point, maybe the power of Lisp macros could be useful for shaders.

## Usage

To compile the file `frag.lsl` to `frag.spv.asm`, call the `lsslc` Compiler like this:

    lsslc -o frag.spv.asm frag.lsl

The generated file is in human readable SPIR-V assembly representation and needs to be assembled by `spirv-as` before it can be used.

    spirv-as frag.spv.asm -o frag.spv

## Example

```lisp
(defversion 460 core)

(defout FragColor vec4
  {:layout {:location 0}})

(defuniform inputs Inputs
  {:layout {:memory :std140
            :binding 0}}
  (color vec4))

(defun void main []
  (reset! FragColor (getx inputs color)))
```

## Development

A few useful commands when working on the compiler:

```
# Running the compiler via clj
clj -M:lsslc ...

# Generating LWJGL dependencies for dev
clj -M:lwjgl/deps

# MAC-OS repl
clj -M:cider-repl
# and then cider-connect-clj in Emacs
```

## License

Copyright © 2020 Markus Walther

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
