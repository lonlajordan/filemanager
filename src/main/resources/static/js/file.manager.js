let $ = jQuery;
let ctx = $("meta[name='ctx']").attr("content");
let options = {
    background: 'rgba(255, 255, 255, 0.75)'
}

function selectItems(checkBox){
    $("#main-table tbody tr input[type=checkbox]").each(function () { this.checked = checkBox.checked; })
}

function reverseSelection(){
    $("#main-table tbody tr input[type=checkbox]").each(function () { this.checked = !this.checked; })
}

function selectItem(checkBox){
    if(!checkBox.checked) $("#js-select-all-items").prop( "checked", false);
}

function deleteItem(id, url){
    if(confirm("Voulez vous vraiment supprimer cet élément ?")){
        fetch(ctx + '/' + url + '/' + id, false);
    }
    $("#modal-delete").modal('show');
}

function deleteItems(url){
    let selected = $("#main-table tbody tr input[type=checkbox]:checked");
    let n = selected.length;
    if(n === 0){
        alert('Aucun élément sélectionné');
    }else{
        let params = [];
        selected.each(function () {
            params.push('ids=' + $(this).attr('title'));
        });
        params = params.join("&");
       /* if(confirm("Voulez-vous vraiment supprimer " + (n === 1 ? 'cet élément ?' : 'ces ' + n + ' éléments ?'))){
            fetch(ctx + '/' + url + '?' + params, false);
        }*/
        $("#modal-delete").modal('show');
    }
}

function deleteFile(path, isDirectory){
    $("#delete-message").text("Voulez-vous vraiment supprimer ce " + (isDirectory ? "dossier" :  "fichier") + " ?");
    let container = document.getElementById("delete-files");
    container.innerHTML = '';
    let input = document.createElement("input");
    input.type = "hidden";
    input.name = "paths";
    input.value = path;
    container.appendChild(input);
    $("#modal-delete").modal('show');
}

function renameFile(path, oldName) {
    $("#file-path").val(path);
    $("#new-name").val(oldName);
    $("#modal-rename").modal('show');
}

function createFolder(event) {
    let folderName = prompt("Nom du dossier", "Nouveau dossier");
    if(folderName){
        let href = $("#create-folder").attr('href');
        let index = href.lastIndexOf('&name');
        if(index < 0){
            href += "&name=" + folderName;
        }else{
            href = href.substring(0, index) + "&name=" + folderName;
        }
        $("#create-folder").attr("href", href);
    }else{
        event.preventDefault();
    }
}

function invoke(action) {
    let paths = $.makeArray($("#main-table tbody input[type=checkbox]:checked")).map(checkbox => $(checkbox).val());
    if(paths === undefined || paths.length === 0){
        new SnackBar({
            message: 'Aucun élément sélectionné',
            status: 'error',
            dismissible: false,
            position: 'bc',
            timeout: 3000,
        });
    }else{
        if(action === 'download'){
            downloadFile(paths, '/download/files');
        }else if(action === 'copy' || action === 'cut'){
            let params = paths.map(path => 'paths=' + path).join('&');
            window.location = ctx + '/move/files?action=' + action + '&' + params;
        }else if(action === 'delete'){
            $("#delete-message").text("Voulez-vous vraiment supprimer " + (paths.length < 2 ? "cet élément" :  "ces " + paths.length + " éléments") + " ?");
            let container = document.getElementById("delete-files");
            container.innerHTML = '';
            for(let path of paths){
                let input = document.createElement("input");
                input.type = "hidden";
                input.name = "paths";
                input.value = path;
                container.appendChild(input);
            }
            $("#modal-delete").modal('show');
        }
    }
}

function downloadFile(paths, url) {
    $("#wrapper").LoadingOverlay('show', options);
    let xhr = new XMLHttpRequest();
    let params = paths.map(path => 'path=' + path).join('&');
    xhr.open('GET', ctx + url + '?' + params, true);
    xhr.responseType = 'arraybuffer';
    xhr.onload = function() {
        if(this.status === 200) {
            let filename = '';
            let disposition = xhr.getResponseHeader('Content-Disposition');
            if (disposition && disposition.indexOf('attachment') !== -1) {
                let filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                let matches = filenameRegex.exec(disposition);
                if (matches !== null && matches[1]) filename = matches[1].replace(/['"]/g, '');
            }
            let type = xhr.getResponseHeader('Content-Type');
            let blob = new Blob([this.response],  {type: type});
            //workaround for IE
            if(typeof window.navigator.msSaveBlob != 'undefined') {
                window.navigator.msSaveBlob(blob, filename);
            } else {
                let URL = window.URL || window.webkitURL;
                let download_URL = URL.createObjectURL(blob);
                if(filename) {
                    let a_link = document.createElement('a');
                    if(typeof a_link.download == 'undefined') {
                        window.location = download_URL;
                    }else {
                        a_link.href = download_URL;
                        a_link.download = filename;
                        document.body.appendChild(a_link);
                        a_link.click();
                    }
                }else {
                    window.location = download_URL;
                }
                setTimeout(function() {
                    URL.revokeObjectURL(download_URL);
                }, 10000);
            }
        }else {
            new SnackBar({
                message: 'Téléchargement échoué',
                status: 'error',
                dismissible: false,
                position: 'bc',
                timeout: 3000,
            });
        }
        $("#main-table input[type=checkbox]:checked").each(function () { this.checked = false; });
        $('#wrapper').LoadingOverlay('hide', options);
        $('#wrapper').scrollTop(0);
    };
    xhr.setRequestHeader('Content-type', 'application/*');
    xhr.send();
}

$.ajaxSetup({
    beforeSend: function () {
        $("#wrapper").LoadingOverlay('show', options);
    },
    complete: function () {
        $('#wrapper').LoadingOverlay('hide', options);
        $('#wrapper').scrollTop(0);
    }
})

function fetch(url){
    $.get(url, function (data) {});
}

$(document).ready( function () {
    let table = $('#main-table'),
    length = table.find('th').length,
    targets = [length-1];
    let mainTable = table.DataTable({"paging": false, "info": false, "columnDefs": [{"targets": targets, "orderable": false}]});
    $('#search-addon').on('keyup', function () {
        mainTable.search(this.value).draw();
    });
    $(".lazy-link").each(function () {
        let element = this;
        element.addEventListener("click", function (event) {
            event.preventDefault();
            let url = $(this).attr('href');
            fetch(url);
        })
    })
});