# unlam-triviumapp

## Introducción

El cifrado Trivium es una herramienta criptográfica que es utilizada para la encriptación de datos para aplicaciones de alta velocidad, donde la pérdida de una cantidad finita de bits no represente un problema, como puede ser una llamada telefónica o una transmisión de un video.

El cifrador toma como entrada un k-bit clave secreta K y una n-bit IV. Luego se solicita el cifrado para generar hasta 2 ^64 bits, de esta secuencia clave con el texto plano produce el texto cifrado. La seguridad de este cifrado de flujo está determinado por una clave secreta que se usa para que junto a un vector inicializado (IV) se obtenga un flujo de bits que mediante una XOR cifraran los bits del texto plano.

## Especificaciones

Trivium es un cifrado de flujo sincrónico  compuesto por 288 puntos flotantes, 11 puertas XOR y 3 puertas AND y diseñado para generar hasta 2^64 bits de flujo de clave de una clave secreta de 80 bits y un valor inicial de 80 bits (IV).
Los bits de estado se rotan y el proceso se repite hasta que se hayan solicitado los bits de flujo de claves solicitados generado.

La generación de flujo de claves consiste en un proceso iterativo que extrae valores de 15 bits de estado específicos y los usa para actualizar 3 bits del estado y para calcular 1 bit de key stream zi.

Para entender esto, tenemos que mirarlos como 3 registros separados donde cada uno produce su propio output donde el bit como output final es el XOR entre los output de los 3 registros mencionados.

El output de cada registro también se usa para ayudar a formar la entrada de otro registro.

## Inicialización

Al comenzar con trivium, hacemos que los 80 bit de la clave secreta sean los 80 bits del primer registro, completando el resto con “0”. Luego se realiza el mismo procedimiento con el segundo registro en vez de usar el primer registro y el vector inicialización (IV) en vez de la clave secreta. Por último, los últimos 3 bits (los de más a la derecha) del tercer registro se setean en 1.

Dependiendo de la implementación de Trivium la generación de entradas con salidas puede variar entre registros. En la versión clásica de Trivium solo se ingresa la clave secreta para crear el flujo de bits cifradores, pero un pequeño cambio podría ser que también permita ingresar otro vector de inicialización, aunque sería como usar 2 claves secretas, lo cual no le agregaria una mejora significativa más que un mayor secretismo al algoritmo. 
Otras versiones podrían tomar otros bits distintos al 66 y 93 del primer registro, 162 y 177 del segundo, etc.
Este último cambio en la toma de bits para realizar las XOR y AND es más significativo por que los atacantes por más que tuvieran la clave secreta si usan el trivium clásico para tratar de desencriptar el texto cifrado no podrán lograrlo. Cabe destacar que estos cambios son más sencillos de hacer si Trivium está implementado enteramente por software que si es por medio de hardware, ya que al ser por hardware es más performante pero no se puede volver a modificar los circuitos del cifrador.

## Ejemplo

Proponemos que los bits del 1 al 80, 94 al 173 y del 286 al 288 son todos 1, el resto son todos 0.

Comenzamos:
• El output del registro 1 es el XOR del bit 66 (1) y el del bit 93 (0) lo cual da 1.
• El output del registro 2 es el XOR del bit 162 (1) y el del bit 177(0) lo cual da 1
• El output del registro 3 es el XOR del bit 288 (1) y el del bit 243(0) lo cual da 1 

Por lo cual el primer output es 1. Ahora todo debe moverse así que veamos: El input del registro 2 es el XOR entre el bit 171 (1) y el XOR del output del primer registro (1) y el AND entre los bit 91 y 92 (lo cual da 0), entonces eso da que 1 XOR 1 XOR 0 = 0 que va a ser el nuevo input del registro 2. Todos los otros bits del registro 2 se deslizan hacia la derecha. (El registro 2 es ahora 0 en el bit 94, 1 en los bits del 95 al 174 y 0 en el resto de los lugares). Del mismo modo, se puede calcular que el nuevo input del registro 3 va a ser 1 y el nuevo input del registro 1 es un 0.


## Bibliografía

- http://crypto.prof.ninja/class9/
- https://eprint.iacr.org/2009/431.pdf
- http://www.ecrypt.eu.org/stream/p3ciphers/trivium/trivium_p3.pdf
- https://en.wikipedia.org/wiki/Trivium_(cipher)
- https://en.wikipedia.org/wiki/Shift_register
- https://www.youtube.com/watch?v=imZwZa8oIxs
- https://www.youtube.com/watch?v=XRWbS1CJY-0
- https://www.youtube.com/watch?v=YRm2hrPVUgw

## Integrantes

- Nahuel José Roldán (nahu.jose.roldan1990@gmail.com)
- Joel Ciccone (joelsciccone@gmail.com)
- Lucas Ron (ronlucas86@gmail.com)
- Alan Reskin (alanreskin7@gmail.com)