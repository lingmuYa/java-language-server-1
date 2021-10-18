package org.javacs.semantics;

import com.itsaky.lsp.SemanticHighlight;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.lang.model.element.*;

import org.eclipse.lsp4j.Range;
import org.javacs.FileStore;
import org.javacs.markup.RangeHelper;

import static javax.lang.model.element.ElementKind.*;
import static org.javacs.services.JavaLanguageServer.*;

class SemanticHighlighter extends TreePathScanner<Void, SemanticHighlight> {
    private final Trees trees;

    SemanticHighlighter(JavacTask task) {
        trees = Trees.instance(task);
    }
    
    private void putSemantics(Name name, SemanticHighlight colors) {
        if (name.contentEquals("this") || name.contentEquals("super") || name.contentEquals("class")) {
            return;
        }
        var fromPath = getCurrentPath();
        var toEl = trees.getElement(fromPath);
        if (toEl == null) {
            return;
        }
        
        var range = find(fromPath, name);
        if(range == Range_NONE)
            return;
        
        var kind = toEl.getKind();
        
        if(kind == PACKAGE) {
            colors.packages.add(range);
            return;
        }
        
        if(kind == ENUM) {
            colors.enumTypes.add(range);
            return;
        }
        
        if(kind == CLASS) {
            colors.classNames.add(range);
            return;
        }
        
        if(kind == ANNOTATION_TYPE) {
            colors.annotationTypes.add(range);
            return;
        }
        
        if(kind == INTERFACE) {
            colors.interfaces.add(range);
            return;
        }
        
        if(kind == ENUM_CONSTANT) {
            colors.enums.add(range);
            return;
        }
        
        if (kind == FIELD) {
            if (toEl.getModifiers().contains(Modifier.STATIC)) {
                colors.statics.add(range);
            } else {
                colors.fields.add(range);
            }
            return;
        }
        
        if(kind == METHOD) {
        	colors.methodDeclarations.add(range);
        	return;
        }
        
        if(kind == PARAMETER) {
            colors.parameters.add(range);
            return;
        }
        
        if(kind == LOCAL_VARIABLE) {
            colors.locals.add(range);
            return;
        }
        
        if(kind == EXCEPTION_PARAMETER) {
            colors.exceptionParams.add(range);
            return;
        }
        
        if(kind == CONSTRUCTOR) {
            colors.constructors.add(range);
            return;
        }
        
        if(kind == STATIC_INIT) {
            colors.staticInits.add(range);
            return;
        }
        
        if(kind == INSTANCE_INIT) {
            colors.instanceInits.add(range);
            return;
        }
        
        if(kind == TYPE_PARAMETER) {
            colors.typeParams.add(range);
            return;
        }
        
        if(kind == RESOURCE_VARIABLE) {
            colors.resourceVariables.add(range);
            return;
        }
    }

    private Range find(TreePath path, Name name) {
        // Find region containing name
        var pos = trees.getSourcePositions();
        var root = path.getCompilationUnit();
        var leaf = path.getLeaf();
        var start = (int) pos.getStartPosition(root, leaf);
        var end = (int) pos.getEndPosition(root, leaf);
        // Adjust start to remove LHS of declarations and member selections
        if (leaf instanceof MemberSelectTree) {
            var select = (MemberSelectTree) leaf;
            start = (int) pos.getEndPosition(root, select.getExpression());
        } else if (leaf instanceof VariableTree) {
            var declaration = (VariableTree) leaf;
            start = (int) pos.getEndPosition(root, declaration.getType());
        }
        // If no position, give up
        if (start == -1 || end == -1) {
            return Range_NONE;
        }
        // Find name inside expression
        var file = Paths.get(root.getSourceFile().toUri());
        var contents = FileStore.contents(file);
        var region = contents.substring(start, end);
        start += region.indexOf(name.toString());
        end = start + name.length();
        return RangeHelper.range(root, start, end);
    }

    @Override
    public Void visitIdentifier(IdentifierTree t, SemanticHighlight colors) {
        putSemantics(t.getName(), colors);
        return super.visitIdentifier(t, colors);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree t, SemanticHighlight colors) {
        putSemantics(t.getIdentifier(), colors);
        return super.visitMemberSelect(t, colors);
    }

    @Override
    public Void visitVariable(VariableTree t, SemanticHighlight colors) {
        putSemantics(t.getName(), colors);
        return super.visitVariable(t, colors);
    }

    @Override
    public Void visitClass(ClassTree t, SemanticHighlight colors) {
        putSemantics(t.getSimpleName(), colors);
        return super.visitClass(t, colors);
    }
    
    @Override
    public Void visitMethodInvocation (MethodInvocationTree tree, SemanticHighlight colors) {
    	Name name = null;
    	var select = tree.getMethodSelect();
    	if(select instanceof MemberSelectTree) {
    		name = ((MemberSelectTree) select).getIdentifier();
    	} else if(select instanceof IdentifierTree) {
    		name = ((IdentifierTree) select).getName();
    	}
    	
    	if(name != null) {
    		var range = find(getCurrentPath(), name);
    		if(range != null) {
    			colors.methodInvocations.add(range);
    		}
    	}
		return super.visitMethodInvocation(tree, colors);
    	
    }
    
    @Override
    public Void visitMethod(MethodTree tree, SemanticHighlight colors) {
    	putSemantics(tree.getName(), colors);
		return super.visitMethod(tree, colors);
	}
    
    private static final Logger LOG = Logger.getLogger("main");
}