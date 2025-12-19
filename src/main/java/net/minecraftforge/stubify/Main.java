/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.stubify;

import joptsimple.OptionParser;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFile.ConstantPoolSharingOption;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {
    private static final ClassDesc RUNTIME_EXCEPTION = ClassDesc.of(RuntimeException.class.getName());
    private static final Checksum CRC32 = new CRC32();

    public static void main(String[] args) throws Exception {
        var parser = new OptionParser();
        var inputO = parser.accepts("input", "Input file")
                .withRequiredArg().ofType(File.class).required();
        var outputO = parser.accepts("output", "Output file")
                .withRequiredArg().ofType(File.class).required();
        var storeO = parser.accepts("store", "Uses STORED method, disabling compression");
        var helpO = parser.accepts("help")
                .forHelp();

        var options = parser.parse(args);

        if (options.has(helpO)) {
            parser.printHelpOn(System.out);
            return;
        }

        var input = options.valueOf(inputO);
        var output = options.valueOf(outputO);
        var store = options.has(storeO);

        System.out.println("Input:  " + (input == null ? "null" : input.getAbsolutePath()));
        System.out.println("Output: " + (output == null ? "null" : output.getAbsolutePath()));
        System.out.println("Store:  " + store);

        if (!input.exists())
            throw new IllegalArgumentException("Input file does not exist: " + input);

        var parent = output.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdir();

        if (output.exists())
            output.delete();

        try (
            var zipIn = new ZipFile(input);
            var zipOut = new ZipOutputStream(new FileOutputStream(output));
        ) {
            if (store)
                zipOut.setMethod(ZipOutputStream.STORED); // Different compression libraries cause different final data. So just store

            for (var itr = zipIn.entries().asIterator(); itr.hasNext(); ) {
                var entry = itr.next();
                // We only want class files
                if (!entry.getName().endsWith(".class"))
                    continue;

                System.out.println("Processing " + entry.getName());

                var data = zipIn.getInputStream(entry).readAllBytes();

                var classTransform = ClassTransform.transformingMethodBodies(new CodeTransform() {
                    @Override
                    public void accept(CodeBuilder builder, CodeElement element) {
                    }

                    @Override
                    public void atEnd(CodeBuilder builder) {
                        builder
                            .new_(RUNTIME_EXCEPTION)
                            .dup()
                            .invokespecial(RUNTIME_EXCEPTION, "<init>", ConstantDescs.MTD_void)
                            .athrow();
                    }
                });

                var classFile = ClassFile.of()
                    .withOptions(ConstantPoolSharingOption.NEW_POOL);

                var transformed = classFile.transformClass(classFile.parse(data), classTransform);

                var next = new ZipEntry(entry.getName());
                next.setTime(entry.getTime());
                if (store) {
                    next.setSize(transformed.length);
                    next.setCompressedSize(transformed.length);
                    next.setCrc(crc32(transformed));
                }

                zipOut.putNextEntry(next);
                zipOut.write(transformed);
                zipOut.closeEntry();
            }
        }
    }

    private static long crc32(byte[] data) {
        CRC32.reset();
        CRC32.update(data);
        return CRC32.getValue();
    }
}
