package com.simpligility.maven.plugins.android.asm;

import java.util.concurrent.atomic.AtomicBoolean;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Finds classes annotated with a set of annotations.
 *
 * @author secondsun@gmail.com
 */
public class AnnotatedFinder extends ClassVisitor 
{

    private static final String TEST_RUNNER = "Lorg/junit/runner/RunWith;";
    
    public AnnotatedFinder( String[] parentPackages ) 
    {
        super( Opcodes.ASM4 );
    }

    private final AtomicBoolean isDescendantFound = new AtomicBoolean( false );

    @Override
    public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
    {
    }

    private void flagAsFound()
    {
        isDescendantFound.set( true );
    }

    /**
     * Returns whether a match was found.
     *
     * @return <code>true</code> is a match was found, <code>false</code> otherwise.
     */
    public boolean isDescendantFound()
    {
        return isDescendantFound.get();
    }

    @Override
    public void visitSource( String source, String debug )
    {
    }

    @Override
    public void visitOuterClass( String owner, String name, String desc )
    {
    }

    @Override
    public AnnotationVisitor visitAnnotation( String desc, boolean visible )
    {
        if ( TEST_RUNNER.equals( desc ) ) 
        {
            return new AnnotationVisitor( Opcodes.ASM4 ) 
            {

                @Override
                public void visit( String name, Object value ) 
                {
                    if ( value instanceof Type ) 
                    {
                        if ( ( ( Type ) value ).getClassName().contains( "AndroidJUnit4" ) ) 
                        {
                            flagAsFound();
                        }
                    }
                }
                
            };
        } 
        return null;
    }

    @Override
    public void visitAttribute( Attribute attr )
    {
    }

    @Override
    public void visitInnerClass( String name, String outerName, String innerName, int access )
    {
    }

    @Override
    public FieldVisitor visitField( int access, String name, String desc, String signature, Object value )
    {
        return null;
    }

    @Override
    public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions )
    {
        return null;
    }

    @Override
    public void visitEnd()
    {
    }
    
}
